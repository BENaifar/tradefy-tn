package tn.esprit.pi5eme.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Password must be 8-64 characters long and include at least one uppercase letter, one lowercase letter, one digit, and one special character (@#$%^&+=!._-).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

