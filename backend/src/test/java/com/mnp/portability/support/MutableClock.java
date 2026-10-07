package com.mnp.portability.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock tests can move forward, so the 2-minute timeout is exercised without waiting. */
public class MutableClock extends Clock {

    private final Instant start;
    private volatile Instant now;

    public MutableClock(Instant start) {
        this.start = start;
        this.now = start;
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    public void reset() {
        now = start;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}