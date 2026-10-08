package com.mnp.portability.phonenumber;

import com.mnp.portability.common.security.CurrentOperator;
import com.mnp.portability.operator.Operator;
import com.mnp.portability.phonenumber.dto.PhoneNumberStatusResponse;
import com.mnp.portability.phonenumber.validation.ValidEgyptianMobile;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/phone-numbers")
@RequiredArgsConstructor
public class PhoneNumberController {

    private final PhoneNumberService phoneNumberService;

    @Operation(summary = "Get a phone number's status and current holder",
            description = "Reports whether the number was ported and which operator holds it now. "
                    + "A pending request is only reported to its donor and recipient.")
    @GetMapping("/{phoneNumber}")
    public PhoneNumberStatusResponse getStatus(@PathVariable @ValidEgyptianMobile String phoneNumber,
                                               @CurrentOperator Operator caller) {
        return phoneNumberService.getStatus(phoneNumber, caller);
    }
}