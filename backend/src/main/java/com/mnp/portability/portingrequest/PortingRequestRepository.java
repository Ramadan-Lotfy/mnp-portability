package com.mnp.portability.portingrequest;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortingRequestRepository extends JpaRepository<PortingRequest, Long> {

    boolean existsByPhoneNumberAndStatus(String phoneNumber, PortingStatus status);

   
    Optional<PortingRequest> findFirstByPhoneNumberAndStatusOrderByUpdatedAtDescIdDesc(
            String phoneNumber, PortingStatus status);
}