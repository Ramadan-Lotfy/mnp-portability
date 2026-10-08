package com.mnp.portability.phonenumber.dto;

import com.mnp.portability.phonenumber.PhoneNumberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record PhoneNumberStatusResponse(
        @Schema(example = "01012345678")
        String phoneNumber,

        PhoneNumberStatus status,

        @Schema(description = "Operator currently holding the number", example = "orange")
        String currentOperator,

        @Schema(description = "Operator whose range the number was allocated from", example = "vodafone")
        String originalOperator) {
}