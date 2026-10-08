package com.mnp.portability.portingrequest;

import static com.mnp.portability.support.TestData.ETISALAT;
import static com.mnp.portability.support.TestData.NOW;
import static com.mnp.portability.support.TestData.ORANGE;
import static com.mnp.portability.support.TestData.VODAFONE;
import static com.mnp.portability.support.TestData.fixedClock;
import static com.mnp.portability.support.TestData.pendingRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mnp.portability.common.config.PortingProperties;
import com.mnp.portability.common.exception.BusinessRuleException;
import com.mnp.portability.common.exception.ForbiddenOperationException;
import com.mnp.portability.common.exception.ResourceNotFoundException;
import com.mnp.portability.phonenumber.PhoneNumberService;
import com.mnp.portability.portingrequest.dto.CreatePortingRequest;
import com.mnp.portability.portingrequest.dto.PortingRequestResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PortingRequestServiceTest {

    private static final String NUMBER = "01012345678";
    private static final Duration TIMEOUT = Duration.ofMinutes(2);

    @Mock
    private PortingRequestRepository repository;
    @Mock
    private PhoneNumberService phoneNumberService;

    private PortingRequestService service;

    @BeforeEach
    void setUp() {
        service = new PortingRequestService(
                repository,
                phoneNumberService,
                new PortingRequestMapper(),
                new PortingProperties(TIMEOUT, Duration.ofSeconds(15)),
                fixedClock());
    }

    // ---- submit -------------------------------------------------------------------------------

    @Test
    void submitOpensPendingRequestFromCurrentHolderToCaller() {
        given(phoneNumberService.findCurrentHolder(NUMBER)).willReturn(Optional.of(VODAFONE));
        given(repository.existsByPhoneNumberAndStatus(NUMBER, PortingStatus.PENDING)).willReturn(false);
        given(repository.saveAndFlush(any(PortingRequest.class))).willAnswer(returnsFirstArg());

        PortingRequestResponse response = service.submit(new CreatePortingRequest(NUMBER), ORANGE);

        assertThat(response.status()).isEqualTo(PortingStatus.PENDING);
        assertThat(response.phoneNumber()).isEqualTo(NUMBER);
        assertThat(response.donor()).isEqualTo("vodafone");
        assertThat(response.recipient()).isEqualTo("orange");
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void submitRejectsNumberOutsideEveryRangeWith422() {
        given(phoneNumberService.findCurrentHolder("01500000000")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(new CreatePortingRequest("01500000000"), ORANGE))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT));
        verify(repository, never()).saveAndFlush(any(PortingRequest.class));
    }

    @Test
    void submitRejectsNumberTheRecipientAlreadyHoldsWith422() {
        given(phoneNumberService.findCurrentHolder(NUMBER)).willReturn(Optional.of(ORANGE));

        assertThatThrownBy(() -> service.submit(new CreatePortingRequest(NUMBER), ORANGE))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT));
        verify(repository, never()).saveAndFlush(any(PortingRequest.class));
    }

    @Test
    void submitRejectsNumberWithPendingRequestWith409() {
        given(phoneNumberService.findCurrentHolder(NUMBER)).willReturn(Optional.of(VODAFONE));
        given(repository.existsByPhoneNumberAndStatus(NUMBER, PortingStatus.PENDING)).willReturn(true);

        assertThatThrownBy(() -> service.submit(new CreatePortingRequest(NUMBER), ORANGE))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(repository, never()).saveAndFlush(any(PortingRequest.class));
    }

    @Test
    void submitReports409WhenTheDatabaseIndexLosesARaceForTheSameNumber() {
        given(phoneNumberService.findCurrentHolder(NUMBER)).willReturn(Optional.of(VODAFONE));
        given(repository.existsByPhoneNumberAndStatus(NUMBER, PortingStatus.PENDING)).willReturn(false);
        given(repository.saveAndFlush(any(PortingRequest.class)))
                .willThrow(new DataIntegrityViolationException("duplicate pending_phone_number"));

        assertThatThrownBy(() -> service.submit(new CreatePortingRequest(NUMBER), ORANGE))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---- accept / reject ----------------------------------------------------------------------

    @Test
    void donorCanAccept() {
        PortingRequest request = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW.minusSeconds(30));
        given(repository.findById(1L)).willReturn(Optional.of(request));
        given(repository.saveAndFlush(any(PortingRequest.class))).willAnswer(returnsFirstArg());

        PortingRequestResponse response = service.accept(1L, VODAFONE);

        assertThat(response.status()).isEqualTo(PortingStatus.ACCEPTED);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void donorCanReject() {
        PortingRequest request = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW.minusSeconds(30));
        given(repository.findById(1L)).willReturn(Optional.of(request));
        given(repository.saveAndFlush(any(PortingRequest.class))).willAnswer(returnsFirstArg());

        PortingRequestResponse response = service.reject(1L, VODAFONE);

        assertThat(response.status()).isEqualTo(PortingStatus.REJECTED);
    }

    @Test
    void recipientCannotDecideItsOwnRequest() {
        PortingRequest request = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW);
        given(repository.findById(1L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> service.accept(1L, ORANGE)).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.reject(1L, ORANGE)).isInstanceOf(ForbiddenOperationException.class);
        assertThat(request.isPending()).isTrue();
    }

    @Test
    void thirdPartyGetsNotFoundForAPendingRequest() {
        PortingRequest request = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW);
        given(repository.findById(1L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> service.accept(1L, ETISALAT)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(request.isPending()).isTrue();
    }

    @Test
    void unknownIdIsNotFound() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(99L, VODAFONE)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void alreadyDecidedRequestCannotBeDecidedAgain() {
        PortingRequest request = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW);
        request.reject(NOW);
        given(repository.findById(1L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> service.accept(1L, VODAFONE))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(repository, never()).saveAndFlush(any(PortingRequest.class));
    }

    @Test
    void requestPastTheTimeoutCannotBeDecidedEvenBeforeTheSweepRuns() {
        PortingRequest request = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW.minus(TIMEOUT).minusSeconds(1));
        given(repository.findById(1L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> service.accept(1L, VODAFONE))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getMessage()).contains("timed out");
                });
        verify(repository, never()).saveAndFlush(any(PortingRequest.class));
    }

    @Test
    void requestExactlyAtTheTimeoutBoundaryCanStillBeDecided() {
        PortingRequest request = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW.minus(TIMEOUT));
        given(repository.findById(1L)).willReturn(Optional.of(request));
        given(repository.saveAndFlush(any(PortingRequest.class))).willAnswer(returnsFirstArg());

        assertThat(service.accept(1L, VODAFONE).status()).isEqualTo(PortingStatus.ACCEPTED);
    }

    // ---- timeout sweep ------------------------------------------------------------------------

    @Test
    void cancelExpiredCancelsEveryStaleRequestAndReportsTheCount() {
        Instant cutoff = NOW.minus(TIMEOUT);
        List<PortingRequest> stale = List.of(
                pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW.minus(Duration.ofMinutes(3))),
                pendingRequest(2L, "01112345678", ETISALAT, VODAFONE, NOW.minus(Duration.ofMinutes(5))));
        given(repository.findByStatusAndCreatedAtBefore(PortingStatus.PENDING, cutoff)).willReturn(stale);

        int cancelled = service.cancelExpired();

        assertThat(cancelled).isEqualTo(2);
        assertThat(stale).allSatisfy(request -> {
            assertThat(request.getStatus()).isEqualTo(PortingStatus.CANCELED);
            assertThat(request.getUpdatedAt()).isEqualTo(NOW);
        });
        verify(repository).saveAllAndFlush(stale);
    }

    @Test
    void cancelExpiredDoesNothingWhenNothingIsStale() {
        given(repository.findByStatusAndCreatedAtBefore(PortingStatus.PENDING, NOW.minus(TIMEOUT)))
                .willReturn(List.of());

        assertThat(service.cancelExpired()).isZero();
    }
}