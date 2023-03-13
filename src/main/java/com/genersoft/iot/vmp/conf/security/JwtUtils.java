package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.security.dto.JwtUser;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private static final String HEADER = "Access-Token";
    private static final String AUDIENCE = "Audience";

    private static final long EXPIRED_THRESHOLD = 10 * 60;

    private static final String keyId = "3e79646c4dbc408383a9eed09f2b85ae";
    private static final String privateKeyStr = "{\"kty\":\"RSA\",\"kid\":\"3e79646c4dbc408383a9eed09f2b85ae\",\"alg\":\"RS256\",\"n\":\"gndmVdiOTSJ5et2HIeTM5f1m61x5ojLUi5HDfvr-jRrESQ5kbKuySGHVwR4QhwinpY1wQqBnwc80tx7cb_6SSqsTOoGln6T_l3k2Pb54ClVnGWiW_u1kmX78V2TZOsVmZmwtdZCMi-2zWIyAdIEXE-gncIehoAgEoq2VAhaCURbJWro_EwzzQwNmCTkDodLAx4npXRd_qSu0Ayp0txym9OFovBXBULRvk4DPiy3i_bPUmCDxzC46pTtFOe9p82uybTehZfULZtXXqRm85FL9n5zkrsTllPNAyEGhgb0RK9sE5nK1m_wNNysDyfLC4EFf1VXTrKm14XNVjc2vqLb7Mw\",\"e\":\"AQAB\",\"d\":\"ed7U_k3rJ4yTk70JtRSIfjKGiEb67BO1TabcymnljKO7RU8nage84zZYuSu_XpQsHk6P1f0Gzxkicghm_Er-FrfVn2pp70Xu52z3yRd6BJUgWLDFk97ngScIyw5OiULKU9SrZk2frDpftNCSUcIgb50F8m0QAnBa_CdPsQKbuuhLv8V8tBAV7F_lAwvSBgu56wRo3hPz5dWH8YeXM7XBfQ9viFMNEKd21sP_j5C7ueUnXT66nBxe3ZJEU3iuMYM6D6dB_KW2GfZC6WmTgvGhhxJD0h7aYmfjkD99MDleB7SkpbvoODOqiQ5Epb7Nyh6kv5u4KUv2CJYtATLZkUeMkQ\",\"p\":\"uBUjWPWtlGksmOqsqCNWksfqJvMcnP_8TDYN7e4-WnHL4N-9HjRuPDnp6kHvCIEi9SEfxm7gNxlRcWegvNQr3IZCz7TnCTexXc5NOklB9OavWFla6u-s3Thn6Tz45-EUjpJr0VJMxhO-KxGmuTwUXBBp4vN6K2qV6rQNFmgkWzk\",\"q\":\"tW_i7cCec56bHkhITL_79dXHz_PLC_f7xlynmlZJGU_d6mqOKmLBNBbTMLnYW8uAFiFzWxDeDHh1o5uF0mSQR-Z1Fg35OftnpbWpy0Cbc2la5WgXQjOwtG1eLYIY2BD3-wQ1VYDBCvowr4FDi-sngxwLqvwmrJ0xjhi99O-Gzcs\",\"dp\":\"q1d5jE85Hz_6M-eTh_lEluEf0NtPEc-vvhw-QO4V-cecNpbrCBdTWBmr4dE3NdpFeJc5ZVFEv-SACyei1MBEh0ItI_pFZi4BmMfy2ELh8ptaMMkTOESYyVy8U7veDq9RnBcr5i1Nqr0rsBkA77-9T6gzdvycBZdzLYAkAmwzEvk\",\"dq\":\"q29A2K08Crs-jmp2Bi8Q_8QzvIX6wSBbwZ4ir24AO-5_HNP56IrPS0yV2GCB0pqCOGb6_Hz_koDvhtuYoqdqvMVAtMoXR3YJBUaVXPt65p4RyNmFwIPe31zHs_BNUTsXVRMw4c16mci03-Af1sEm4HdLfxAp6sfM3xr5wcnhcek\",\"qi\":\"rHPgVTyHUHuYzcxfouyBfb1XAY8nshwn0ddo81o1BccD4Z7zo5It6SefDHjxCAbcmbiCcXBSooLcY-NF5FMv3fg19UE21VyLQltHcVjRRp2tRs4OHcM8yaXIU2x6N6Z6BP2tOksHb9MOBY1wAQzFOAKg_G4Sxev6-_6ud6RISuc\"}";
    private static final String publicKeyStr = "{\"kty\":\"RSA\",\"kid\":\"3e79646c4dbc408383a9eed09f2b85ae\",\"alg\":\"RS256\",\"n\":\"gndmVdiOTSJ5et2HIeTM5f1m61x5ojLUi5HDfvr-jRrESQ5kbKuySGHVwR4QhwinpY1wQqBnwc80tx7cb_6SSqsTOoGln6T_l3k2Pb54ClVnGWiW_u1kmX78V2TZOsVmZmwtdZCMi-2zWIyAdIEXE-gncIehoAgEoq2VAhaCURbJWro_EwzzQwNmCTkDodLAx4npXRd_qSu0Ayp0txym9OFovBXBULRvk4DPiy3i_bPUmCDxzC46pTtFOe9p82uybTehZfULZtXXqRm85FL9n5zkrsTllPNAyEGhgb0RK9sE5nK1m_wNNysDyfLC4EFf1VXTrKm14XNVjc2vqLb7Mw\",\"e\":\"AQAB\"}";

    /**
     * token过期时间(分钟)
     */
    public static final long expirationTime = 30;

    public static String createToken(String username, String password) {
        try {
            /**
             * “iss” (issuer)  发行人
             *
             * “sub” (subject)  主题
             *
             * “aud” (audience) 接收方 用户
             *
             * “exp” (expiration time) 到期时间
             *
             * “nbf” (not before)  在此之前不可用
             *
             * “iat” (issued at)  jwt的签发时间
             */
            //Payload
            JwtClaims claims = new JwtClaims();
            claims.setGeneratedJwtId();
            claims.setIssuedAtToNow();
            // 令牌将过期的时间 分钟
            claims.setExpirationTimeMinutesInTheFuture(expirationTime);
            claims.setNotBeforeMinutesInThePast(0);
            claims.setSubject("login");
            claims.setAudience(AUDIENCE);
            //添加自定义参数,必须是字符串类型
            claims.setClaim("username", username);
            claims.setClaim("password", password);

            //jws
            JsonWebSignature jws = new JsonWebSignature();
            //签名算法RS256
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
            jws.setKeyIdHeaderValue(keyId);
            jws.setPayload(claims.toJson());

            PrivateKey privateKey = new RsaJsonWebKey(JsonUtil.parseJson(privateKeyStr)).getPrivateKey();
            jws.setKey(privateKey);

            //get token
            String idToken = jws.getCompactSerialization();
            return idToken;
        } catch (JoseException e) {
            logger.error("[Token生成失败]： {}", e.getMessage());
        }

        return null;
    }

    public static String getHeader() {
        return HEADER;
    }


    public static JwtUser verifyToken(String token) {

        JwtUser jwtUser = new JwtUser();

        try {
            JwtConsumer consumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setMaxFutureValidityInMinutes(5256000)
                    .setAllowedClockSkewInSeconds(30)
                    .setRequireSubject()
                    //.setExpectedIssuer("")
                    .setExpectedAudience(AUDIENCE)
                    .setVerificationKey(new RsaJsonWebKey(JsonUtil.parseJson(publicKeyStr)).getPublicKey())
                    .build();

            JwtClaims claims = consumer.processToClaims(token);
            NumericDate expirationTime = claims.getExpirationTime();
            // 判断是否即将过期, 默认剩余时间小于5分钟未即将过期
            // 剩余时间 （秒）
            long timeRemaining = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8)) - expirationTime.getValue();
            if (timeRemaining < 5 * 60) {
                jwtUser.setStatus(JwtUser.TokenStatus.EXPIRING_SOON);
            }else {
                jwtUser.setStatus(JwtUser.TokenStatus.NORMAL);
            }

            String username = (String) claims.getClaimValue("username");
            String password = (String) claims.getClaimValue("password");
            jwtUser.setUserName(username);
            jwtUser.setPassword(password);

            return jwtUser;
        } catch (InvalidJwtException e) {
            if (e.hasErrorCode(ErrorCodes.EXPIRED)) {
                jwtUser.setStatus(JwtUser.TokenStatus.EXPIRED);
            }else {
                jwtUser.setStatus(JwtUser.TokenStatus.EXCEPTION);
            }
            return jwtUser;
        }catch (Exception e) {
            logger.error("[Token解析失败]： {}", e.getMessage());
            jwtUser.setStatus(JwtUser.TokenStatus.EXPIRED);
            return jwtUser;
        }
    }
}
