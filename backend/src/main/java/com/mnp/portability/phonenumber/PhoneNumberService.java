package com.mnp.portability.phonenumber;

import com.mnp.portability.common.exception.ResourceNotFoundException;
import com.mnp.portability.operator.NumberRange;
import com.mnp.portability.operator.NumberRangeRepository;
import com.mnp.portability.operator.Operator;
import com.mnp.portability.phonenumber.dto.PhoneNumberStatusResponse;
import com.mnp.portability.portingrequest.PortingRequest;
import com.mnp.portability.portingrequest.PortingRequestRepository;
import com.mnp.portability.portingrequest.PortingStatus;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhoneNumberService {

    private final PortingRequestRepository portingRequestRepository;
    private final NumberRangeRepository numberRangeRepository;

    /**
     * The operator that currently holds a number: the recipient of its latest accepted porting,
     * or, if it was never ported, the operator whose range it was allocated from.
     * Empty when the number belongs to no known range.
     */
    @Transactional(readOnly = true)
    public Optional<Operator> findCurrentHolder(String phoneNumber) {
        return latestAcceptedRecipient(phoneNumber).or(() -> findOriginalOperator(phoneNumber));
    }

    /**
     * Status of a number as the caller is allowed to see it. A pending request is only reported to
     * its own donor and recipient, matching the visibility rule for porting requests.
     *
     * @throws ResourceNotFoundException if the number belongs to no known operator range
     */
    @Transactional(readOnly = true)
    public PhoneNumberStatusResponse getStatus(String phoneNumber, Operator caller) {
        Operator original = findOriginalOperator(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Phone number %s does not belong to any known operator range".formatted(phoneNumber)));
        Operator current = latestAcceptedRecipient(phoneNumber).orElse(original);

        boolean pendingVisibleToCaller = portingRequestRepository.findPending(phoneNumber)
                .filter(pending -> pending.involves(caller))
                .isPresent();

        PhoneNumberStatus status;
        if (pendingVisibleToCaller) {
            status = PhoneNumberStatus.PORTING_PENDING;
        } else if (current.getId().equals(original.getId())) {
            status = PhoneNumberStatus.NOT_PORTED;
        } else {
            status = PhoneNumberStatus.PORTED;
        }

        return new PhoneNumberStatusResponse(phoneNumber, status, current.getCode(), original.getCode());
    }

    /** The operator whose range the number was allocated from, ignoring any porting. */
    private Optional<Operator> findOriginalOperator(String phoneNumber) {
        return numberRangeRepository.findContaining(phoneNumber).map(NumberRange::getOperator);
    }

    /** The recipient of the number's latest accepted porting, if it was ever ported. */
    private Optional<Operator> latestAcceptedRecipient(String phoneNumber) {
        return portingRequestRepository
                .findFirstByPhoneNumberAndStatusOrderByUpdatedAtDescIdDesc(phoneNumber, PortingStatus.ACCEPTED)
                .map(PortingRequest::getRecipient);
    }
}