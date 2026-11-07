package tn.esprit.pi5eme;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.pi5eme.security.JwtUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000L); // 1 hour
    }

    @Test
    @DisplayName("Should generate valid JWT and successfully extract subject and validate token")
    void shouldGenerateAndValidateToken() {
        String email = "testuser@esprit.tn";
        String token = jwtUtils.generateTokenFromUsername(email, 1L, "ROLE_USER");

        assertNotNull(token);
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals(email, jwtUtils.getUsernameFromJwtToken(token));
    }

    @Test
    @DisplayName("Should fail validation on invalid or tampered JWT")
    void shouldFailValidationOnTamperedToken() {
        String token = jwtUtils.generateTokenFromUsername("user@esprit.tn", 1L, "ROLE_USER");
        String tampered = token + "corrupted";

        assertFalse(jwtUtils.validateJwtToken(tampered));
    }
}

