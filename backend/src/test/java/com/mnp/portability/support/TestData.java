package com.mnp.portability.support;

import com.mnp.portability.operator.NumberRange;
import com.mnp.portability.operator.Operator;
import com.mnp.portability.portingrequest.PortingRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** Shared fixtures for unit and slice tests. Entities have no public constructors, so they are built reflectively. */
public final class TestData {

    public static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");

    // Same ids as the Flyway seed data.
    public static final Operator VODAFONE = operator(1L, "vodafone", "Vodafone");
    public static final Operator ETISALAT = operator(2L, "etisalat", "Etisalat");
    public static final Operator ORANGE = operator(3L, "orange", "Orange");

    private TestData() {
    }

    public static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    public static Operator operator(long id, String code, String name) {
        Operator operator = BeanUtils.instantiateClass(Operator.class);
        ReflectionTestUtils.setField(operator, "id", id);
        ReflectionTestUtils.setField(operator, "code", code);
        ReflectionTestUtils.setField(operator, "name", name);
        return operator;
    }

    public static NumberRange rangeOf(Operator operator) {
        NumberRange range = BeanUtils.instantiateClass(NumberRange.class);
        ReflectionTestUtils.setField(range, "operator", operator);
        return range;
    }

    public static PortingRequest pendingRequest(long id, String phoneNumber, Operator donor,
                                                Operator recipient, Instant createdAt) {
        PortingRequest request = PortingRequest.open(phoneNumber, donor, recipient, createdAt);
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }
}