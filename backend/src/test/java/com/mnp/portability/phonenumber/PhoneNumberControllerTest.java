package com.mnp.portability.phonenumber;

import static com.mnp.portability.support.TestData.ORANGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mnp.portability.common.config.ClockConfig;
import com.mnp.portability.common.exception.ResourceNotFoundException;
import com.mnp.portability.operator.Operator;
import com.mnp.portability.operator.OperatorService;
import com.mnp.portability.phonenumber.dto.PhoneNumberStatusResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PhoneNumberController.class)
@Import(ClockConfig.class)
class PhoneNumberControllerTest {

    private static final String URL = "/api/v1/phone-numbers/";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhoneNumberService phoneNumberService;
    @MockitoBean
    private OperatorService operatorService;

    @BeforeEach
    void knownOperators() {
        given(operatorService.findByCode("orange")).willReturn(Optional.of(ORANGE));
    }

    @Test
    void returnsStatusAndCurrentHolder() throws Exception {
        given(phoneNumberService.getStatus(any(String.class), any(Operator.class)))
                .willReturn(new PhoneNumberStatusResponse(
                        "01012345678", PhoneNumberStatus.PORTED, "orange", "vodafone"));

        mockMvc.perform(get(URL + "01012345678").header("organization", "orange"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PORTED"))
                .andExpect(jsonPath("$.currentOperator").value("orange"))
                .andExpect(jsonPath("$.originalOperator").value("vodafone"));
    }

    @Test
    void malformedNumberIs400() throws Exception {
        mockMvc.perform(get(URL + "123").header("organization", "orange"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void numberOutsideEveryRangeIs404() throws Exception {
        given(phoneNumberService.getStatus(any(String.class), any(Operator.class)))
                .willThrow(new ResourceNotFoundException("not in any range"));

        mockMvc.perform(get(URL + "01500000000").header("organization", "orange"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingOrganizationHeaderIs401() throws Exception {
        mockMvc.perform(get(URL + "01012345678"))
                .andExpect(status().isUnauthorized());
    }
}