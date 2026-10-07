package com.mnp.portability.phonenumber.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;


public class EgyptianMobileValidator implements ConstraintValidator<ValidEgyptianMobile, String> {

    private static final Pattern FORMAT = Pattern.compile("^01\\d{9}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && FORMAT.matcher(value).matches();
    }
}