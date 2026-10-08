package com.mnp.portability.portingrequest.dto;

import com.mnp.portability.phonenumber.validation.ValidEgyptianMobile;
import io.swagger.v3.oas.annotations.media.Schema;


public record CreatePortingRequest(
        @Schema(description = "The single phone number to port", example = "01012345678")
        @ValidEgyptianMobile
        String phoneNumber) {
}