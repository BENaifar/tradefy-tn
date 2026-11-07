package tn.esprit.pi5eme.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    /**
     * Regex requirements:
     * - Minimum 8 characters, maximum 64 characters
     * - At least one lowercase letter
     * - At least one uppercase letter
     * - At least one numeric digit
     * - At least one special symbol: @#$%^&+=!._-
     * - No whitespace allowed
     */
    public static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._\\-])(?=\\S+$).{8,64}$";

    private static final Pattern PATTERN = Pattern.compile(PASSWORD_PATTERN);

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        return PATTERN.matcher(password).matches();
    }
}

