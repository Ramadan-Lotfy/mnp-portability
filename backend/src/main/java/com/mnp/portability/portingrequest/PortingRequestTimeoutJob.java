package com.mnp.portability.portingrequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background task from the porting flow diagram: cancels pending requests that the donor
 * has not answered within the configured timeout. The rule itself lives in the service;
 * this class only decides when it runs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortingRequestTimeoutJob {

    private final PortingRequestService portingRequestService;

    @Scheduled(fixedDelayString = "${porting.timeout-check-interval}")
    public void cancelExpiredRequests() {
        try {
            int cancelled = portingRequestService.cancelExpired();
            if (cancelled > 0) {
                log.info("Canceled {} porting request(s) that timed out without a donor decision", cancelled);
            }
        } catch (OptimisticLockingFailureException ex) {
            // A donor decided on one of the requests at the same moment; the next run starts clean.
            log.warn("Timeout sweep lost a race with a donor decision; it will be retried on the next run");
        }
    }
}