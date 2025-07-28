package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.dto.JwtUser;
import com.genersoft.iot.vmp.service.IUserApiKeyService;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.storager.dao.dto.UserApiKey;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtUtils implements InitializingBean {

    public static final String HEADER = "access-token";

    public static final String API_KEY_HEADER = "api-key";

    private static final String AUDIENCE = "Audience";

    private static final String keyId = "3e79646c4dbc408383a9eed09f2b85ae";

    /**
     * token过期时间(分钟)
     */
    public static final long EXPIRATION_TIME = 30;

    private static RsaJsonWebKey rsaJsonWebKey;

    private static IUserService userService;

    private static IUserApiKeyService userApiKeyService;
    
    private static UserSetting userSetting;

    public static String getApiKeyHeader() {
        return API_KEY_HEADER;
    }

    @Resource
    public void setUserService(IUserService userService) {
        JwtUtils.userService = userService;
    }

    @Resource
    public void setUserApiKeyService(IUserApiKeyService userApiKeyService) {
        JwtUtils.userApiKeyService = userApiKeyService;
    }

    @Resource
    public void setUserSetting(UserSetting userSetting) {
        JwtUtils.userSetting = userSetting;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            rsaJsonWebKey = generateRsaJsonWebKey();
        } catch (JoseException e) {
            log.error("生成RsaJsonWebKey报错。", e);
        }
    }

    /**
     * 创建密钥对
     *
     * @throws JoseException JoseException
     */
    private RsaJsonWebKey generateRsaJsonWebKey() throws JoseException {
        RsaJsonWebKey rsaJsonWebKey = null;
        try {
            String jwkFile = userSetting.getJwkFile();
            InputStream inputStream = null;
            if (jwkFile.startsWith("classpath:")){
                String filePath = jwkFile.substring("classpath:".length());
                ClassPathResource civilCodeFile = new ClassPathResource(filePath);
                if (civilCodeFile.exists()) {
                    inputStream = civilCodeFile.getInputStream();
                }
            }else {
                File civilCodeFile = new File(userSetting.getCivilCodeFile());
                if (civilCodeFile.exists()) {
                    inputStream = Files.newInputStream(civilCodeFile.toPath());
                }

            }
            if (inputStream == null ) {
                log.warn("[API AUTH] 读取jwk.json失败，文件不存在，将使用新生成的随机RSA密钥对");
                // 生成一个RSA密钥对，该密钥对将用于JWT的签名和验证，包装在JWK中
                rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
                // 给JWK一个密钥ID
                rsaJsonWebKey.setKeyId(keyId);
                return rsaJsonWebKey;
            }
            BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            int index = -1;
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = inputStreamReader.readLine()) != null) {
                content.append(line);
                index ++;
                if (index == 0) {
                    continue;
                }
            }
            inputStreamReader.close();
            inputStream.close();


            String jwkJson = content.toString();
            JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(jwkJson);
            List<JsonWebKey> jsonWebKeys = jsonWebKeySet.getJsonWebKeys();
            if (!jsonWebKeys.isEmpty()) {
                JsonWebKey jsonWebKey = jsonWebKeys.get(0);
                if (jsonWebKey instanceof RsaJsonWebKey) {
                    rsaJsonWebKey = (RsaJsonWebKey) jsonWebKey;
                }
            }
        } catch (Exception ignore) {}
        if (rsaJsonWebKey == null) {
            log.warn("[API AUTH] 读取jwk.json失败，获取内容失败，将使用新生成的随机RSA密钥对");
            // 生成一个RSA密钥对，该密钥对将用于JWT的签名和验证，包装在JWK中
            rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
            // 给JWK一个密钥ID
            rsaJsonWebKey.setKeyId(keyId);
        }else {
            log.info("[API AUTH] 读取jwk.json成功");
        }
        return rsaJsonWebKey;
    }

    public static String createToken(String username, Long expirationTime, Map<String, Object> extra) {
        try {
            /*
             * “iss” (issuer)  发行人
             * “sub” (subject)  主题
             * “aud” (audience) 接收方 用户
             * “exp” (expiration time) 到期时间
             * “nbf” (not before)  在此之前不可用
             * “iat” (issued at)  jwt的签发时间
             */
            JwtClaims claims = new JwtClaims();
            claims.setGeneratedJwtId();
            claims.setIssuedAtToNow();
            // 令牌将过期的时间 分钟
            if (expirationTime != null) {
                claims.setExpirationTimeMinutesInTheFuture(expirationTime);
            }
            claims.setNotBeforeMinutesInThePast(0);
            claims.setSubject("login");
            claims.setAudience(AUDIENCE);
            //添加自定义参数,必须是字符串类型
            claims.setClaim("userName", username);
            if (extra != null) {
                extra.forEach(claims::setClaim);
            }
            //jws
            JsonWebSignature jws = new JsonWebSignature();
            //签名算法RS256
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
            jws.setKeyIdHeaderValue(keyId);
            jws.setPayload(claims.toJson());

            jws.setKey(rsaJsonWebKey.getPrivateKey());

            //get token
            return jws.getCompactSerialization();
        } catch (JoseException e) {
            log.error("[Token生成失败]： {}", e.getMessage());
        }
        return null;
    }

    public static String createToken(String username, Long expirationTime) {
        return createToken(username, expirationTime, null);
    }

    public static String createToken(String username) {
        return createToken(username, userSetting.getLoginTimeout());
    }

    public static String getHeader() {
        return HEADER;
    }

    public static JwtUser verifyToken(String token) {

        JwtUser jwtUser = new JwtUser();

        try {
            JwtConsumer consumer = new JwtConsumerBuilder()
                    //.setRequireExpirationTime()
                    //.setMaxFutureValidityInMinutes(5256000)
                    .setAllowedClockSkewInSeconds(30)
                    .setRequireSubject()
                    //.setExpectedIssuer("")
                    .setExpectedAudience(AUDIENCE)
                    .setVerificationKey(rsaJsonWebKey.getPublicKey())
                    .build();

            JwtClaims claims = consumer.processToClaims(token);
            NumericDate expirationTime = claims.getExpirationTime();
            if (expirationTime != null) {
                // 判断是否即将过期, 默认剩余时间小于5分钟未即将过期
                // 剩余时间 （秒）
                long timeRemaining = expirationTime.getValue() - LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8));
                if (timeRemaining < 5 * 60) {
                    jwtUser.setStatus(JwtUser.TokenStatus.EXPIRING_SOON);
                } else {
                    jwtUser.setStatus(JwtUser.TokenStatus.NORMAL);
                }
            } else {
                jwtUser.setStatus(JwtUser.TokenStatus.NORMAL);
            }

            Long apiKeyId = claims.getClaimValue("apiKeyId", Long.class);
            if (apiKeyId != null) {
                UserApiKey userApiKey = userApiKeyService.getUserApiKeyById(apiKeyId.intValue());
                if (userApiKey == null || !userApiKey.isEnable()) {
                    jwtUser.setStatus(JwtUser.TokenStatus.EXPIRED);
                }
            }

            String username = (String) claims.getClaimValue("userName");
            User user = userService.getUserByUsername(username);

            jwtUser.setUserName(username);
            jwtUser.setPassword(user.getPassword());
            jwtUser.setRoleId(user.getRole().getId());
            jwtUser.setUserId(user.getId());

            return jwtUser;
        } catch (InvalidJwtException e) {
            if (e.hasErrorCode(ErrorCodes.EXPIRED)) {
                jwtUser.setStatus(JwtUser.TokenStatus.EXPIRED);
            } else {
                jwtUser.setStatus(JwtUser.TokenStatus.EXCEPTION);
            }
            return jwtUser;
        } catch (Exception e) {
            log.error("[Token解析失败]： {}", e.getMessage());
            jwtUser.setStatus(JwtUser.TokenStatus.EXPIRED);
            return jwtUser;
        }
    }
}
