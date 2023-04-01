package org.apache.dubbo.metrics.registry.event.type;

import org.apache.dubbo.metrics.model.MetricsKey;

public enum ApplicationType {
    R_TOTAL(MetricsKey.REGISTER_METRIC_REQUESTS),
    R_SUCCEED(MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED),
    R_FAILED(MetricsKey.REGISTER_METRIC_REQUESTS_FAILED),

    S_TOTAL(MetricsKey.SUBSCRIBE_METRIC_NUM),
    S_SUCCEED(MetricsKey.SUBSCRIBE_METRIC_NUM_SUCCEED),
    S_FAILED(MetricsKey.SUBSCRIBE_METRIC_NUM_FAILED),

    D_VALID(MetricsKey.DIRECTORY_METRIC_NUM_VALID),
    D_UN_VALID(MetricsKey.DIRECTORY_METRIC_NUM_UN_VALID),
    D_DISABLE(MetricsKey.DIRECTORY_METRIC_NUM_DISABLE),
    D_CURRENT(MetricsKey.DIRECTORY_METRIC_NUM_CURRENT),
    D_RECOVER_DISABLE(MetricsKey.DIRECTORY_METRIC_NUM_RECOVER_DISABLE),

    N_TOTAL(MetricsKey.NOTIFY_METRIC_REQUESTS),
    ;

    private final MetricsKey metricsKey;


    ApplicationType(MetricsKey metricsKey) {
        this.metricsKey = metricsKey;
    }

    public MetricsKey getMetricsKey() {
        return metricsKey;
    }

}
