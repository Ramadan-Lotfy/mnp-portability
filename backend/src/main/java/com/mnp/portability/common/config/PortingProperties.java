package com.mnp.portability.common.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Business settings for porting requests, bound from the {@code porting.*} keys in application.yaml.
 * Registered by {@code @ConfigurationPropertiesScan} on the application class.
 *
 * @param requestTimeout       how long a request may stay PENDING before it is cancelled
 * @param timeoutCheckInterval how long the cleanup job waits between runs
 */
@Validated
@ConfigurationProperties(prefix = "porting")
public record PortingProperties(
        @NotNull Duration requestTimeout,
        @NotNull Duration timeoutCheckInterval) {
}