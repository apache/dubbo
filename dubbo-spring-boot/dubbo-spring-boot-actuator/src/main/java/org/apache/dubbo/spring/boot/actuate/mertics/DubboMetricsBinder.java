package org.apache.dubbo.spring.boot.actuate.mertics;

import org.apache.dubbo.metrics.DubboMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author songxiaosheng
 */
public class DubboMetricsBinder implements ApplicationListener<ApplicationStartedEvent>, DisposableBean {
    private final MeterRegistry meterRegistry;
    private volatile DubboMetrics dubboMetrics;

    public DubboMetricsBinder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }


    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {
         dubboMetrics = new DubboMetrics();
         dubboMetrics.bindTo(meterRegistry);
    }

    @Override
    public void destroy() throws Exception {
        dubboMetrics.destroy();
    }
}
