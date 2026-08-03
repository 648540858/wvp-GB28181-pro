package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.UserSetting;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    private UserSetting userSetting;

    @BeforeAll
    static void initializeJwtKey() throws Exception {
        ReflectionTestUtils.setField(JwtUtils.class, "rsaJsonWebKey", RsaJwkGenerator.generateJwk(2048));
    }

    @BeforeEach
    void setUp() {
        userSetting = new UserSetting();
        ReflectionTestUtils.setField(JwtUtils.class, "userSetting", userSetting);
    }

    @Test
    void shouldCreateNonExpiringTokenWhenLoginTimeoutIsZero() throws Exception {
        userSetting.setLoginTimeout(0L);

        JwtClaims claims = claims(JwtUtils.createToken("admin"));

        assertNull(claims.getExpirationTime());
    }

    @Test
    void shouldCreateTokenValidForTwoHoursByDefault() throws Exception {
        userSetting.setLoginTimeout(120L);

        JwtClaims claims = claims(JwtUtils.createToken("admin"));
        long validitySeconds = claims.getExpirationTime().getValue() - claims.getIssuedAt().getValue();

        assertTrue(validitySeconds >= 7199 && validitySeconds <= 7201);
    }

    private JwtClaims claims(String token) throws Exception {
        return new JwtConsumerBuilder()
                .setSkipSignatureVerification()
                .setSkipAllValidators()
                .build()
                .processToClaims(token);
    }
}
