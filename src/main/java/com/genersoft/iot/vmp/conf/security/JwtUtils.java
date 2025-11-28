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

import jakarta.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * 创建密钥对（修复所有bug+classpath警告+密钥持久化）
     */
    private RsaJsonWebKey generateRsaJsonWebKey() throws JoseException {
        // 前置校验：避免空指针（防止userSetting未初始化或jwkFile未配置）
        if (userSetting == null) {
            log.error("[API AUTH] userSetting 未初始化！");
            return createDefaultRsaKey();
        }
        String jwkFile = userSetting.getJwkFile();
        if (jwkFile == null || jwkFile.trim().isEmpty()) {
            log.error("[API AUTH] JWK文件路径未配置！");
            return createDefaultRsaKey();
        }

        // 尝试读取JWK文件（自动处理classpath/本地文件，用try-with-resources自动关流，无泄露）
        try (InputStream inputStream = getJwkInputStream(jwkFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            // 读取JSON（不跳过任何行，修复原bug）
            String jwkJson = reader.lines().collect(Collectors.joining());
            JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(jwkJson);
            List<JsonWebKey> jsonWebKeys = jsonWebKeySet.getJsonWebKeys();

            // 筛选：取第一个有效的RSA私钥（签名需要私钥，避免后续报错）
            for (JsonWebKey jsonWebKey : jsonWebKeys) {
                if (jsonWebKey instanceof RsaJsonWebKey) {
                    RsaJsonWebKey rsaKey = (RsaJsonWebKey) jsonWebKey;
                    // 校验是否包含私钥
                    if (rsaKey.getPrivateKey() != null) {
                        log.info("[API AUTH] 从JWK文件读取RSA密钥成功，keyId: {}", rsaKey.getKeyId());
                        return rsaKey;
                    }
                }
            }
            log.error("[API AUTH] JWK文件中无有效RSA私钥（仅公钥无法签名JWT）");

        } catch (IOException e) {
            log.error("[API AUTH] 读取JWK文件失败（路径：{}）", jwkFile, e);
        } catch (Exception e) {
            log.error("[API AUTH] 解析JWK文件失败（JSON格式错误或密钥无效）", e);
        }

        // 所有失败场景：生成默认密钥并持久化（避免重启失效）
        return createAndPersistDefaultRsaKey(jwkFile);
    }

    /**
     * 获取JWK文件输入流（支持classpath/本地文件，classpath读取加安全警告）
     */
    private InputStream getJwkInputStream(String jwkFile) throws IOException {
        if (jwkFile.startsWith("classpath:")) {
            String filePath = jwkFile.substring("classpath:".length());
            ClassPathResource resource = new ClassPathResource(filePath);
            if (resource.exists()) {
                // 关键：classpath读取时打印安全警告，提醒用户确认密钥来源
                log.warn("[API AUTH] 从classpath读取内置JWK文件：{}！请确认该密钥是您自己签发的，" +
                        "classpath内置密钥存在泄露风险，生产环境建议改用外部文件配置", filePath);
                return resource.getInputStream();
            }
            // throw new IOException("classpath下JWK文件不存在：" + filePath);
        } 
        {
            File file = determinePersistPath(jwkFile).toFile();// 外部配置与classpath失败场景下
            if (file.exists() && file.canRead()) {
                log.debug("[API AUTH] 从本地文件读取JWK文件：{}", file.getAbsolutePath());
                return Files.newInputStream(file.toPath());
            }
            throw new IOException("本地JWK文件不存在或无读取权限：" + file.getAbsolutePath());
        }
    }

        /**
     * 生成默认RSA密钥（单独抽取，修复之前漏写的问题）
     */
    private RsaJsonWebKey createDefaultRsaKey() throws JoseException {
        RsaJsonWebKey defaultKey = RsaJwkGenerator.generateJwk(4096);
        defaultKey.setKeyId(keyId);
        log.warn("[API AUTH] 使用默认生成的RSA密钥（未持久化，重启会失效），keyId: {}", defaultKey.getKeyId());
        return defaultKey;
    }

    /**
     * 生成默认RSA密钥并持久化到文件（修复原重复代码，避免重启失效）
     */
    private RsaJsonWebKey createAndPersistDefaultRsaKey(String configJwkFile) throws JoseException {
        // 1. 生成4096位RSA密钥（原2048位升级，更安全）
        RsaJsonWebKey defaultKey = RsaJwkGenerator.generateJwk(4096);
        defaultKey.setKeyId(keyId); // keyId配置

        // 2. 确定持久化路径：优先用户配置的非classpath路径，否则用默认外部路径
        Path persistPath = determinePersistPath(configJwkFile);
        if (persistPath == null) {
            log.warn("[API AUTH] 生成默认RSA密钥（keyId: {}），但配置路径是classpath（只读）！" +
                    "服务重启后密钥会失效，请修改jwkFile为外部可写路径（如：/opt/config/jwk.json）", defaultKey.getKeyId());
            return defaultKey;
        }

        // 3. 保存密钥到文件（标准JWK Set格式，下次启动可直接读取）
        try {
            // 自动创建父目录（比如./config不存在时会自动建）
            Files.createDirectories(persistPath.getParent());
            // 构建标准JWK Set JSON（jose4j的toString()自带正确格式）
            JsonWebKeySet jwkSet = new JsonWebKeySet(defaultKey);
            String jwkJson = jwkSet.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE);
            // 写入文件（覆盖已有文件，避免重复）
            Files.writeString(persistPath, jwkJson, StandardCharsets.UTF_8);
            log.info("[API AUTH] 生成默认RSA密钥（keyId: {}）并持久化到：{}",
                    defaultKey.getKeyId(), persistPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("[API AUTH] 生成默认RSA密钥成功，但持久化失败（路径：{}）！服务重启后密钥会失效",
                    persistPath.toAbsolutePath(), e);
        }

        return defaultKey;
    }

    /**
     * 确定密钥持久化路径（兼容classpath只读场景）
     */
    private Path determinePersistPath(String configJwkFile) {
        // 若配置路径不是classpath，直接用用户配置的路径（外部可写）
        if (!configJwkFile.startsWith("classpath:")) {
            return Paths.get(configJwkFile);
        }
        // 若配置是classpath，保存到默认外部路径：./config/jwk.json（项目根目录下的config文件夹）
        Path defaultPath = Paths.get("config", "jwk.json");
        log.warn("[API AUTH] 配置的jwkFile是classpath路径（只读），默认密钥将保存到外部路径：{}",
                defaultPath.toAbsolutePath());
        return defaultPath;
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
