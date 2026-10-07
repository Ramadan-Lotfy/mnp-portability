package com.mnp.portability.portingrequest;

import com.mnp.portability.operator.Operator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PortingRequestRepository extends JpaRepository<PortingRequest, Long> {

    /** Fast pre-check for the "one pending request per number" rule (the DB index is the final guard). */
    boolean existsByPhoneNumberAndStatus(String phoneNumber, PortingStatus status);

    /**
     * The most recent request for a number in the given status. With {@link PortingStatus#ACCEPTED}
     * this identifies the operator currently holding a ported number.
     */
    Optional<PortingRequest> findFirstByPhoneNumberAndStatusOrderByUpdatedAtDescIdDesc(
            String phoneNumber, PortingStatus status);

    /** The pending request for a number, if any. There is at most one: the database enforces it. */
    @Query("""
            select r from PortingRequest r
            where r.phoneNumber = :phoneNumber
              and r.status = com.mnp.portability.portingrequest.PortingStatus.PENDING
            """)
    Optional<PortingRequest> findPending(@Param("phoneNumber") String phoneNumber);

    /** Requests in the given status created strictly before the cutoff; used to find expired PENDING ones. */
    List<PortingRequest> findByStatusAndCreatedAtBefore(PortingStatus status, Instant cutoff);

    /**
     * Requests the operator is allowed to see: everything where it is the donor or recipient, plus
     * every accepted request. Mirrors {@link PortingRequest#isVisibleTo(Operator)}.
     *
     * @param status optional filter; {@code null} means any status
     */
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