package org.apache.dubbo.metrics.spi;

import org.apache.dubbo.common.constants.SpiMethods;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.MetricsEventBus;

public class PublishMetricsEvent implements SpiMethod {

    @Override
    public SpiMethods methodName() {
        return SpiMethods.toRsEvent;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }

    /**
     * {@link MetricsEventBus#publish(MetricsEvent)}
     */
    @Override
    public Object invoke(Object... params) {
        MetricsEventBus.publish((MetricsEvent) params[0]);
        return null;
    }
}
