package com.mnp.portability.portingrequest;

import static com.mnp.portability.support.TestData.ETISALAT;
import static com.mnp.portability.support.TestData.NOW;
import static com.mnp.portability.support.TestData.ORANGE;
import static com.mnp.portability.support.TestData.VODAFONE;
import static com.mnp.portability.support.TestData.pendingRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PortingRequestTest {

    private static final String NUMBER = "01012345678";

    @Test
    void opensInPendingState() {
        PortingRequest request = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW);

        assertThat(request.getStatus()).isEqualTo(PortingStatus.PENDING);
        assertThat(request.isPending()).isTrue();
        assertThat(request.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void pendingRequestCanBeAcceptedRejectedOrCanceled() {
        Instant later = NOW.plusSeconds(30);

        PortingRequest accepted = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW);
        accepted.accept(later);
        PortingRequest rejected = pendingRequest(2L, NUMBER, VODAFONE, ORANGE, NOW);
        rejected.reject(later);
        PortingRequest canceled = pendingRequest(3L, NUMBER, VODAFONE, ORANGE, NOW);
        canceled.cancel(later);

        assertThat(accepted.getStatus()).isEqualTo(PortingStatus.ACCEPTED);
        assertThat(rejected.getStatus()).isEqualTo(PortingStatus.REJECTED);
        assertThat(canceled.getStatus()).isEqualTo(PortingStatus.CANCELED);
        assertThat(accepted.getUpdatedAt()).isEqualTo(later);
        assertThat(accepted.getCreatedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @EnumSource(value = PortingStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void decidedRequestCannotChangeAgain(PortingStatus terminalStatus) {
        PortingRequest request = requestIn(terminalStatus);

        assertThatIllegalStateException().isThrownBy(() -> request.accept(NOW));
        assertThatIllegalStateException().isThrownBy(() -> request.reject(NOW));
        assertThatIllegalStateException().isThrownBy(() -> request.cancel(NOW));
        assertThat(request.getStatus()).isEqualTo(terminalStatus);
    }

    @Test
    void identifiesDonorAndParties() {
        PortingRequest request = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW);

        assertThat(request.isDonor(VODAFONE)).isTrue();
        assertThat(request.isDonor(ORANGE)).isFalse();
        assertThat(request.involves(VODAFONE)).isTrue();
        assertThat(request.involves(ORANGE)).isTrue();
        assertThat(request.involves(ETISALAT)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(PortingStatus.class)
    void donorAndRecipientSeeTheRequestInAnyStatus(PortingStatus status) {
        PortingRequest request = requestIn(status);

        assertThat(request.isVisibleTo(VODAFONE)).isTrue();
        assertThat(request.isVisibleTo(ORANGE)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(PortingStatus.class)
    void otherOperatorsSeeOnlyAcceptedRequests(PortingStatus status) {
        PortingRequest request = requestIn(status);

        assertThat(request.isVisibleTo(ETISALAT)).isEqualTo(status == PortingStatus.ACCEPTED);
    }

    private static PortingRequest requestIn(PortingStatus status) {
        PortingRequest request = pendingRequest(1L, NUMBER, VODAFONE, ORANGE, NOW);
        switch (status) {
            case ACCEPTED -> request.accept(NOW);
            case REJECTED -> request.reject(NOW);
            case CANCELED -> request.cancel(NOW);
            case PENDING -> { }
        }
        return request;
    }
}