package org.apache.dubbo.metrics.registry.event.type;

import org.apache.dubbo.metrics.model.MetricsKey;

public enum ServiceType {

    N_LAST_NUM(MetricsKey.NOTIFY_METRIC_NUM_LAST),

    R_SERVICE_TOTAL(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS),
    R_SERVICE_SUCCEED(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED),
    R_SERVICE_FAILED(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED),

    S_SERVICE_TOTAL(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM),
    S_SERVICE_SUCCEED(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED),
    S_SERVICE_FAILED(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED),
    ;

    private final MetricsKey metricsKey;

    ServiceType(MetricsKey metricsKey) {
        this.metricsKey = metricsKey;
    }

    public MetricsKey getMetricsKey() {
        return metricsKey;
    }

}
