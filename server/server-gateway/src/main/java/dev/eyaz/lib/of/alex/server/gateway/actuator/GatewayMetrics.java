package dev.eyaz.lib.of.alex.server.gateway.actuator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GatewayMetrics {

    private final MeterRegistry meterRegistry;

    public GatewayMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordError(String errorCode, int status) {
        Counter.builder("gateway.errors")
                .description("Count of requests rejected or failed by the Gateway")
                .tag("errorCode", errorCode)
                .tag("status", String.valueOf(status))
                .register(meterRegistry)
                .increment();
    }

    public void recordAuthFailure(String reasonCode) {
        Counter.builder("gateway.auth.failures")
                .description("Count of access token validation failures by reason")
                .tag("reason", reasonCode)
                .register(meterRegistry)
                .increment();
    }

    public void recordRateLimitRejection() {
        Counter.builder("gateway.ratelimit.rejections")
                .description("Count of requests rejected because the rate limit was exceeded")
                .register(meterRegistry)
                .increment();
    }

    public void recordRateLimitFailOpen() {
        Counter.builder("gateway.ratelimit.failopen")
                .description("Count of requests let through without rate limiting because Redis was unreachable")
                .register(meterRegistry)
                .increment();
    }

    public void recordPublicKeyFetchFailure() {
        Counter.builder("gateway.publickey.fetch.failures")
                .description("Count of failures fetching/refreshing the RSA public key from ServiceAuth")
                .register(meterRegistry)
                .increment();
    }

    public void recordLoadBalancerInstanceSelected(String service, String instanceId) {
        Counter.builder("gateway.loadbalancer.selections")
                .description("Count of times a downstream instance was selected by the load balancer")
                .tag("service", service)
                .tag("instance", instanceId)
                .register(meterRegistry)
                .increment();
    }


}

