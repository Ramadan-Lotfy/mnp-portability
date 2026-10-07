package com.mnp.portability.phonenumber.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = EgyptianMobileValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEgyptianMobile {

    String message() default "must be an 11-digit Egyptian mobile number, e.g. 01012345678";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}