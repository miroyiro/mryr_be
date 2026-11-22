package com.miroyiro.backend.global.security.jwt;

import io.jsonwebtoken.JwtException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        byte[] secretBytes = new byte[48];
        new SecureRandom().nextBytes(secretBytes);

        String secret = Base64.getEncoder()
                .encodeToString(secretBytes);

        jwtProvider = new JwtProvider(secret);
    }

    @Test
    void createAccessToken_success() {
        String token = jwtProvider.createAccessToken(
                1L,
                "user"
        );

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals(
                3,
                token.split("\\.").length
        );
    }

    @Test
    void validateAccessToken_success() {
        String token = jwtProvider.createAccessToken(
                1L,
                "user"
        );

        var claims = jwtProvider.validateAccessToken(token);

        assertEquals("1", claims.getSubject());
        assertEquals("user", claims.get("role"));
        assertEquals("access", claims.get("token_type"));
    }

    @Test
    void getUserId_success() {
        String token = jwtProvider.createAccessToken(
                123L,
                "user"
        );

        assertEquals(
                123L,
                jwtProvider.getUserId(token)
        );
    }

    @Test
    void invalidToken_throwsException() {
        assertThrows(
                JwtException.class,
                () -> jwtProvider.validateAccessToken(
                        "invalid.jwt.token"
                )
        );
    }

    @Test
    void tamperedToken_throwsException() {
        String token = jwtProvider.createAccessToken(
                1L,
                "user"
        );

        String[] parts = token.split("\\.");

        // 서명 부분을 변조
        String signature = parts[2];

        char first = signature.charAt(0);

        char replacement = first == 'A' ? 'B' : 'A';

        String tamperedSignature =
                replacement + signature.substring(1);

        String tamperedToken =
                parts[0] + "." +
                parts[1] + "." +
                tamperedSignature;

        assertThrows(
                JwtException.class,
                () -> jwtProvider.validateAccessToken(
                        tamperedToken
                )
        );
    }

    @Test
    void differentSecret_throwsException() {
        String token = jwtProvider.createAccessToken(
                1L,
                "user"
        );

        byte[] otherSecretBytes = new byte[48];
        new SecureRandom().nextBytes(otherSecretBytes);

        JwtProvider anotherProvider = new JwtProvider(
                Base64.getEncoder()
                        .encodeToString(otherSecretBytes)
        );

        assertThrows(
                JwtException.class,
                () -> anotherProvider.validateAccessToken(token)
        );
    }

    @Test
    void accessTokenExpiresIn_is1800Seconds() {
        assertEquals(
                1800L,
                jwtProvider.getAccessTokenExpiresIn()
        );
    }

    @Test
    void shortSecret_throwsException() {
        String shortSecret = Base64.getEncoder()
                .encodeToString(new byte[16]);

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtProvider(shortSecret)
        );
    }
}
