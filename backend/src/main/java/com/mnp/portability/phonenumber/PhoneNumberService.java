package com.mnp.portability.phonenumber;

import com.mnp.portability.operator.NumberRange;
import com.mnp.portability.operator.NumberRangeRepository;
import com.mnp.portability.operator.Operator;
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

  
    @Transactional(readOnly = true)
    public Optional<Operator> findCurrentHolder(String phoneNumber) {
        return portingRequestRepository
                .findFirstByPhoneNumberAndStatusOrderByUpdatedAtDescIdDesc(phoneNumber, PortingStatus.ACCEPTED)
                .map(PortingRequest::getRecipient)
                .or(() -> numberRangeRepository.findContaining(phoneNumber).map(NumberRange::getOperator));
    }
}