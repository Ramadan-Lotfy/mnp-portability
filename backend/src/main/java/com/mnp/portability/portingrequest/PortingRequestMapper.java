package com.mnp.portability.portingrequest;

import com.mnp.portability.portingrequest.dto.PortingRequestResponse;
import org.springframework.stereotype.Component;

@Component
class PortingRequestMapper {

    PortingRequestResponse toResponse(PortingRequest request) {
        return new PortingRequestResponse(
                request.getId(),
                request.getPhoneNumber(),
                request.getDonor().getCode(),
                request.getRecipient().getCode(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}