package com.capitec.fraud.api.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
@Constraint(validatedBy = IsoCurrencyCodeValidator.class)
public @interface IsoCurrencyCode
{

    String message() default "must be a valid ISO currency code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
