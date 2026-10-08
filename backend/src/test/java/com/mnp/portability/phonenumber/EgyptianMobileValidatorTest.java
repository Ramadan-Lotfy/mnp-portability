package com.mnp.portability.phonenumber;

import static org.assertj.core.api.Assertions.assertThat;

import com.mnp.portability.phonenumber.validation.EgyptianMobileValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class EgyptianMobileValidatorTest {

    private final EgyptianMobileValidator validator = new EgyptianMobileValidator();

    @ParameterizedTest
    @ValueSource(strings = {"01000000000", "01012345678", "01199999999", "01299999999"})
    void acceptsElevenDigitNumbersStartingWith01(String number) {
        assertThat(validator.isValid(number, null)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "123",
            "0101234567",        // 10 digits
            "010123456789",      // 12 digits
            "11012345678",       // wrong prefix
            "0101234567a",       // not all digits
            " 01012345678",      // surrounding whitespace
            "+201012345678"      // international format is not accepted
    })
    void rejectsEverythingElse(String number) {
        assertThat(validator.isValid(number, null)).isFalse();
    }
}