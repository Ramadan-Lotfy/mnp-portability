package com.mnp.portability.portingrequest;

import com.mnp.portability.operator.Operator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "porting_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "phone_number", nullable = false, length = 11, updatable = false)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_operator_id", nullable = false, updatable = false)
    private Operator donor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_operator_id", nullable = false, updatable = false)
    private Operator recipient;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PortingStatus status;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;


    public static PortingRequest open(String phoneNumber, Operator donor, Operator recipient, Instant now) {
        PortingRequest request = new PortingRequest();
        request.phoneNumber = phoneNumber;
        request.donor = donor;
        request.recipient = recipient;
        request.status = PortingStatus.PENDING;
        request.createdAt = now;
        request.updatedAt = now;
        return request;
    }

    public boolean isPending() {
        return status == PortingStatus.PENDING;
    }

    public void accept(Instant now) {
        transitionTo(PortingStatus.ACCEPTED, now);
    }

    public void reject(Instant now) {
        transitionTo(PortingStatus.REJECTED, now);
    }

    public void cancel(Instant now) {
        transitionTo(PortingStatus.CANCELED, now);
    }

    private void transitionTo(PortingStatus target, Instant now) {
        if (!isPending()) {
            throw new IllegalStateException(
                    "Porting request %d is %s; only PENDING requests can become %s"
                            .formatted(id, status, target));
        }
        this.status = target;
        this.updatedAt = now;
    }
}