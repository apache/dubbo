package org.apache.dubbo.metrics.model.key;

public class MetricsKeyDecorator {

    private final MetricsKey postKey;
    private final MetricsKey finishKey;
    private final MetricsKey errorKey;
    private final MetricsLevel level;

    public MetricsKeyDecorator(MetricsLevel level, MetricsKey postKey) {
        this(level, postKey, null, null);
    }

    public MetricsKeyDecorator(MetricsLevel level, MetricsKey postKey, MetricsKey finishKey, MetricsKey errorKey) {
        this.level = level;
        this.postKey = postKey;
        this.finishKey = finishKey;
        this.errorKey = errorKey;
    }

    public boolean isAssignableFrom(MetricsKey type) {
        return type == postKey || type == finishKey || type == errorKey;
    }
}
