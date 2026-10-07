package com.mnp.portability.portingrequest;

import com.mnp.portability.common.exception.BusinessRuleException;
import com.mnp.portability.operator.Operator;
import com.mnp.portability.phonenumber.PhoneNumberService;
import com.mnp.portability.portingrequest.dto.CreatePortingRequest;
import com.mnp.portability.portingrequest.dto.PortingRequestResponse;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortingRequestService {

    private final PortingRequestRepository portingRequestRepository;
    private final PhoneNumberService phoneNumberService;
    private final PortingRequestMapper mapper;
    private final Clock clock;

    
    @Transactional
    public PortingRequestResponse submit(CreatePortingRequest command, Operator recipient) {
        String phoneNumber = command.phoneNumber();

        Operator donor = phoneNumberService.findCurrentHolder(phoneNumber)
                .orElseThrow(() -> BusinessRuleException.unprocessable(
                        "Phone number %s does not belong to any known operator range".formatted(phoneNumber)));

        if (donor.getId().equals(recipient.getId())) {
            throw BusinessRuleException.unprocessable(
                    "Phone number %s already belongs to %s".formatted(phoneNumber, recipient.getName()));
        }

        if (portingRequestRepository.existsByPhoneNumberAndStatus(phoneNumber, PortingStatus.PENDING)) {
            throw pendingRequestConflict(phoneNumber);
        }

        PortingRequest request = PortingRequest.open(phoneNumber, donor, recipient, clock.instant());
        try {
            // Flush now so the unique index on pending numbers fires here, inside the try block.
            portingRequestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException ex) {
            // Lost a race with a concurrent request for the same number: the DB is the final guard.
            throw pendingRequestConflict(phoneNumber);
        }
        return mapper.toResponse(request);
    }

    private static BusinessRuleException pendingRequestConflict(String phoneNumber) {
        return BusinessRuleException.conflict(
                "Phone number %s already has a pending porting request".formatted(phoneNumber));
    }
}