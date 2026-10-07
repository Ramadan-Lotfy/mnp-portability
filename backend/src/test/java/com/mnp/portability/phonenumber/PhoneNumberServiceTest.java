package com.mnp.portability.phonenumber;

import static com.mnp.portability.support.TestData.ETISALAT;
import static com.mnp.portability.support.TestData.NOW;
import static com.mnp.portability.support.TestData.ORANGE;
import static com.mnp.portability.support.TestData.VODAFONE;
import static com.mnp.portability.support.TestData.pendingRequest;
import static com.mnp.portability.support.TestData.rangeOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.mnp.portability.common.exception.ResourceNotFoundException;
import com.mnp.portability.operator.NumberRangeRepository;
import com.mnp.portability.phonenumber.dto.PhoneNumberStatusResponse;
import com.mnp.portability.portingrequest.PortingRequest;
import com.mnp.portability.portingrequest.PortingRequestRepository;
import com.mnp.portability.portingrequest.PortingStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhoneNumberServiceTest {

    private static final String NUMBER = "01012345678";   // allocated from Vodafone's range

    @Mock
    private PortingRequestRepository portingRequestRepository;
    @Mock
    private NumberRangeRepository numberRangeRepository;

    private PhoneNumberService service;

    @BeforeEach
    void setUp() {
        service = new PhoneNumberService(portingRequestRepository, numberRangeRepository);
    }

    @Test
    void neverPortedNumberBelongsToTheOperatorOfItsRange() {
        given(numberRangeRepository.findContaining(NUMBER)).willReturn(Optional.of(rangeOf(VODAFONE)));

        PhoneNumberStatusResponse response = service.getStatus(NUMBER, ETISALAT);

        assertThat(response.status()).isEqualTo(PhoneNumberStatus.NOT_PORTED);
        assertThat(response.currentOperator()).isEqualTo("vodafone");
        assertThat(response.originalOperator()).isEqualTo("vodafone");
    }

    @Test
    void portedNumberReportsTheRecipientOfTheLatestAcceptedPorting() {
        given(numberRangeRepository.findContaining(NUMBER)).willReturn(Optional.of(rangeOf(VODAFONE)));
        given(portingRequestRepository.findFirstByPhoneNumberAndStatusOrderByUpdatedAtDescIdDesc(
                NUMBER, PortingStatus.ACCEPTED)).willReturn(Optional.of(acceptedPorting()));

        PhoneNumberStatusResponse response = service.getStatus(NUMBER, ETISALAT);

        assertThat(response.status()).isEqualTo(PhoneNumberStatus.PORTED);
        assertThat(response.currentOperator()).isEqualTo("orange");
        assertThat(response.originalOperator()).isEqualTo("vodafone");
    }

    @Test
    void numberPortedBackToItsOriginalOperatorIsNotPorted() {
        PortingRequest backHome = pendingRequest(2L, NUMBER, ORANGE, VODAFONE, NOW);
        backHome.accept(NOW);
        given(numberRangeRepository.findContaining(NUMBER)).willReturn(Optional.of(rangeOf(VODAFONE)));
        given(portingRequestRepository.findFirstByPhoneNumberAndStatusOrderByUpdatedAtDescIdDesc(
                NUMBER, PortingStatus.ACCEPTED)).willReturn(Optional.of(backHome));

        PhoneNumberStatusResponse response = service.getStatus(NUMBER, ETISALAT);

        assertThat(response.status()).isEqualTo(PhoneNumberStatus.NOT_PORTED);
        assertThat(response.currentOperator()).isEqualTo("vodafone");
    }

    @Test
    void pendingRequestIsReportedToItsDonorAndRecipient() {
        PortingRequest pending = pendingRequest(3L, NUMBER, VODAFONE, ORANGE, NOW);
        given(numberRangeRepository.findContaining(NUMBER)).willReturn(Optional.of(rangeOf(VODAFONE)));
        given(portingRequestRepository.findPending(NUMBER)).willReturn(Optional.of(pending));

        assertThat(service.getStatus(NUMBER, VODAFONE).status()).isEqualTo(PhoneNumberStatus.PORTING_PENDING);
        assertThat(service.getStatus(NUMBER, ORANGE).status()).isEqualTo(PhoneNumberStatus.PORTING_PENDING);
    }

    @Test
    void pendingRequestIsHiddenFromOtherOperators() {
        PortingRequest pending = pendingRequest(3L, NUMBER, VODAFONE, ORANGE, NOW);
        given(numberRangeRepository.findContaining(NUMBER)).willReturn(Optional.of(rangeOf(VODAFONE)));
        given(portingRequestRepository.findPending(NUMBER)).willReturn(Optional.of(pending));

        assertThat(service.getStatus(NUMBER, ETISALAT).status()).isEqualTo(PhoneNumberStatus.NOT_PORTED);
    }

    @Test
    void numberOutsideEveryRangeIsNotFound() {
        given(numberRangeRepository.findContaining("01500000000")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatus("01500000000", ORANGE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void currentHolderOfAPortedNumberIsTheLatestRecipient() {
        given(portingRequestRepository.findFirstByPhoneNumberAndStatusOrderByUpdatedAtDescIdDesc(
                NUMBER, PortingStatus.ACCEPTED)).willReturn(Optional.of(acceptedPorting()));

        assertThat(service.findCurrentHolder(NUMBER)).containsSame(ORANGE);
    }

    @Test
    void currentHolderOfAnUnportedNumberIsTheOperatorOfItsRange() {
        given(numberRangeRepository.findContaining(NUMBER)).willReturn(Optional.of(rangeOf(VODAFONE)));

        assertThat(service.findCurrentHolder(NUMBER)).containsSame(VODAFONE);
    }

    private static PortingRequest acceptedPorting() {
        PortingRequest request = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW);
        request.accept(NOW);
        return request;
    }
}