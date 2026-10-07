package com.mnp.portability.operator;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperatorService {

    private final OperatorRepository operatorRepository;

    public Optional<Operator> findByCode(String code) {
        return operatorRepository.findByCode(code);
    }
}