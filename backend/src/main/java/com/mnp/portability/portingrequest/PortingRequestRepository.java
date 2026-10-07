package com.mnp.portability.portingrequest;

import com.mnp.portability.operator.Operator;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PortingRequestRepository extends JpaRepository<PortingRequest, Long> {

    boolean existsByPhoneNumberAndStatus(String phoneNumber, PortingStatus status);


    Optional<PortingRequest> findFirstByPhoneNumberAndStatusOrderByUpdatedAtDescIdDesc(
            String phoneNumber, PortingStatus status);


    @EntityGraph(attributePaths = {"donor", "recipient"})
    @Query("""
            select r from PortingRequest r
            where (r.status = com.mnp.portability.portingrequest.PortingStatus.ACCEPTED
                   or r.donor = :operator
                   or r.recipient = :operator)
              and (:status is null or r.status = :status)
            """)
    Page<PortingRequest> findVisibleTo(@Param("operator") Operator operator,
                                       @Param("status") PortingStatus status,
                                       Pageable pageable);
}