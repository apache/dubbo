package org.apache.dubbo.metrics.model.container;

import org.apache.dubbo.metrics.model.MetricsKeyWrapper;

import java.util.concurrent.atomic.LongAccumulator;

public class LongAccumulatorContainer extends LongContainer<LongAccumulator> {
    public LongAccumulatorContainer(MetricsKeyWrapper metricsKeyWrapper, LongAccumulator accumulator) {
        super(metricsKeyWrapper, () -> accumulator, (responseTime, longAccumulator) -> longAccumulator.accumulate(responseTime));
    }
}
