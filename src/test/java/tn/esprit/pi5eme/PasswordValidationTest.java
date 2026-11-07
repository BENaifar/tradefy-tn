package tn.esprit.pi5eme;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tn.esprit.pi5eme.validation.PasswordConstraintValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidationTest {

    private final PasswordConstraintValidator validator = new PasswordConstraintValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "Admin@123456",
            "P@ssword1",
            "Super_Secret99!",
            "Complex#Pass2026",
            "User-Test.2024"
    })
    @DisplayName("Should accept valid strong passwords satisfying regex")
    void shouldAcceptValidPasswords(String password) {
        assertTrue(validator.isValid(password, null), "Expected password to be valid: " + password);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short1!",              // less than 8 chars
            "nouppercase123!",       // missing uppercase
            "NOLOWERCASE123!",       // missing lowercase
            "NoSpecialDigits123",    // missing special symbol
            "NoDigits!@#$%",         // missing digit
            "With Space 123!",       // contains whitespace
            ""                       // empty
    })
    @DisplayName("Should reject passwords that do not satisfy regex requirements")
    void shouldRejectInvalidPasswords(String password) {
        assertFalse(validator.isValid(password, null), "Expected password to be invalid: " + password);
    }

    @Test
    @DisplayName("Should reject null password")
    void shouldRejectNullPassword() {
        assertFalse(validator.isValid(null, null));
    }
}

