package com.mnp.portability.portingrequest.dto;

import com.mnp.portability.portingrequest.PortingStatus;
import java.time.Instant;

public record PortingRequestResponse(
        Long id,
        String phoneNumber,
        String donor,
        String recipient,
        PortingStatus status,
        Instant createdAt,
        Instant updatedAt) {
}