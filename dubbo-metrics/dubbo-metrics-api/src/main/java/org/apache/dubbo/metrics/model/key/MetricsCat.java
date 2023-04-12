package org.apache.dubbo.metrics.model.key;

import org.apache.dubbo.metrics.collector.CombMetricsCollector;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.listener.AbstractMetricsListener;

import java.util.function.BiFunction;
import java.util.function.Function;

public class MetricsCat {

    private MetricsPlaceType placeType;
    private final Function<CombMetricsCollector<TimeCounterEvent>, AbstractMetricsListener> eventFunc;

    public MetricsCat(MetricsKey metricsKey, BiFunction<MetricsKey, CombMetricsCollector<TimeCounterEvent>, AbstractMetricsListener> biFunc) {
        this.eventFunc = collector -> biFunc.apply(metricsKey, collector);
    }

    public MetricsCat(MetricsKey metricsKey, TpFunction<MetricsKey, MetricsPlaceType, CombMetricsCollector<TimeCounterEvent>, AbstractMetricsListener> tpFunc) {
        this.eventFunc = collector -> tpFunc.apply(metricsKey, placeType, collector);
    }

    public MetricsCat setPlaceType(MetricsPlaceType placeType) {
        this.placeType = placeType;
        return this;
    }

    public Function<CombMetricsCollector<TimeCounterEvent>, AbstractMetricsListener> getEventFunc() {
        return eventFunc;
    }
}
