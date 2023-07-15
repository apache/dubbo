package org.apache.dubbo.metrics.spi;

import org.apache.dubbo.common.constants.SpiMethodNames;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.MetricsEventBus;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link MetricsEventBus#post(MetricsEvent, Supplier)}
 * {@link MetricsEventBus#post(MetricsEvent, Supplier, Function)}
 */
public class PostMetricsEvent implements SpiMethod {

    @Override
    public SpiMethodNames methodName() {
        return SpiMethodNames.postMetricsEvent;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }


    @Override
    public Object invoke(Object... params) {
        if(params.length == 2){
            MetricsEventBus.post((MetricsEvent) params[0],(Supplier<?>) params[1]);
        }else if(params.length == 3){
            MetricsEventBus.post((MetricsEvent)params[0], (Supplier<?>) params[1], (Function) params[2]);
        }
        return null;
    }
}
