package org.apache.dubbo.metrics.model.container;

import org.apache.dubbo.metrics.model.MetricsKeyWrapper;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

public class AtomicLongContainer extends LongContainer<AtomicLong> {

    public AtomicLongContainer(MetricsKeyWrapper metricsKeyWrapper) {
        super(metricsKeyWrapper, AtomicLong::new, (responseTime, longAccumulator) -> longAccumulator.set(responseTime));
    }

    public AtomicLongContainer(MetricsKeyWrapper metricsKeyWrapper, BiConsumer<Long, AtomicLong> consumerFunc) {
        super(metricsKeyWrapper, AtomicLong::new, consumerFunc);
    }

}
