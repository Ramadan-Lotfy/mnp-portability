package com.mnp.portability.portingrequest;

import com.mnp.portability.common.exception.BusinessRuleException;
import com.mnp.portability.common.exception.ForbiddenOperationException;
import com.mnp.portability.common.exception.ResourceNotFoundException;
import com.mnp.portability.operator.Operator;
import com.mnp.portability.phonenumber.PhoneNumberService;
import com.mnp.portability.portingrequest.dto.CreatePortingRequest;
import com.mnp.portability.portingrequest.dto.PortingRequestResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortingRequestService {

    /** Lists are always newest first; clients cannot choose the sort. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

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

    /** The donor accepts: the recipient becomes the number's holder. */
    @Transactional
    public PortingRequestResponse accept(Long id, Operator caller) {
        return decide(id, caller, PortingRequest::accept);
    }

    /** The donor rejects: the number stays with the donor. */
    @Transactional
    public PortingRequestResponse reject(Long id, Operator caller) {
        return decide(id, caller, PortingRequest::reject);
    }

    /** One request, or 404 if it does not exist or the caller is not allowed to see it. */
    @Transactional(readOnly = true)
    public PortingRequestResponse get(Long id, Operator caller) {
        return mapper.toResponse(findVisible(id, caller));
    }

    /**
     * A page of the requests the caller may see, newest first.
     *
     * @param status optional status filter; {@code null} means any
     */
    @Transactional(readOnly = true)
    public Page<PortingRequestResponse> list(Operator caller, PortingStatus status, Pageable pageable) {
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        return portingRequestRepository.findVisibleTo(caller, status, newestFirst)
                .map(mapper::toResponse);
    }

    private PortingRequestResponse decide(Long id, Operator caller, BiConsumer<PortingRequest, Instant> decision) {
        PortingRequest request = findVisible(id, caller);

        if (!request.isDonor(caller)) {
            throw new ForbiddenOperationException("Only the donor operator can accept or reject a porting request");
        }
        if (!request.isPending()) {
            throw BusinessRuleException.conflict(
                    "Porting request %d is already %s".formatted(id, request.getStatus()));
        }

        decision.accept(request, clock.instant());
        // Flush so a concurrent change (e.g. the timeout job) fails here and not after the response is built.
        portingRequestRepository.saveAndFlush(request);
        return mapper.toResponse(request);
    }

    /** A request that does not exist and one the caller may not see look identical: both are 404. */
    private PortingRequest findVisible(Long id, Operator caller) {
        return portingRequestRepository.findById(id)
                .filter(found -> found.isVisibleTo(caller))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Porting request %d not found".formatted(id)));
    }

    private static BusinessRuleException pendingRequestConflict(String phoneNumber) {
        return BusinessRuleException.conflict(
                "Phone number %s already has a pending porting request".formatted(phoneNumber));
    }
}