package com.company.erp.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enforces a strong password policy: 12–200 chars with upper, lower, digit and symbol.
 * Applied wherever a new password is accepted (registration, change, reset).
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default
            "Password must be 12-200 characters and include upper-case, lower-case, a digit and a symbol";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
