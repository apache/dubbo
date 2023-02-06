package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.TimeAble;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.TimePair;

public class RegistryEvent<S> extends MetricsEvent<S> implements TimeAble {
    private Type type;
    private TimePair timePair;

    public RegistryEvent(S source, Type type) {
        super(source);
        this.type = type;
    }

    public RegistryEvent(S source, TimePair timePair) {
        super(source);
        this.timePair = timePair;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public TimePair getTimePair() {
        return timePair;
    }

    public enum Type {
        TOTAL(MetricsKey.REGISTER_METRIC_REQUESTS),
        SUCCEED(MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED),
        FAILED(MetricsKey.REGISTER_METRIC_REQUESTS_FAILED);

        private final MetricsKey metricsKey;


        Type(MetricsKey metricsKey) {
            this.metricsKey = metricsKey;
        }

        public MetricsKey getMetricsKey() {
            return metricsKey;
        }
    }
}
