package org.apache.dubbo.metrics.model.key;

public class MetricsPlaceType {

    private final String type;
    private final MetricsLevel metricsLevel;

    private MetricsPlaceType(String type, MetricsLevel metricsLevel) {
        this.type = type;
        this.metricsLevel = metricsLevel;
    }

    public static MetricsPlaceType of(String type, MetricsLevel metricsLevel) {
        return new MetricsPlaceType(type, metricsLevel);
    }

    public String getType() {
        return type;
    }

    public MetricsLevel getMetricsLevel() {
        return metricsLevel;
    }
}
