package org.apache.dubbo.metrics.model;

/**
 * Let {@link MetricsKey MetricsKey}  output dynamic, custom string content
 */
public class MetricsKeyWrapper {

    private final String type;
    private final MetricsKey metricsKey;

    public MetricsKeyWrapper(String type, MetricsKey metricsKey) {
        this.type = type;
        this.metricsKey = metricsKey;
    }

    public String getType() {
        return type;
    }

    public MetricsKey getMetricsKey() {
        return metricsKey;
    }

    public boolean isKey(MetricsKey metricsKey) {
        return metricsKey == getMetricsKey();
    }

    public String targetKey() {
        try {
            return String.format(metricsKey.getName(), type);
        } catch (Exception ignore) {
            return metricsKey.getName();
        }
    }

    public String targetDesc() {
        try {
            return String.format(metricsKey.getDescription(), type);
        } catch (Exception ignore) {
            return metricsKey.getDescription();
        }
    }

}
