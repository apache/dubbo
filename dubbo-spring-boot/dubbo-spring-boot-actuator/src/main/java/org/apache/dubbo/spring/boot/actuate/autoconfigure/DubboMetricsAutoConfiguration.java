package org.apache.dubbo.spring.boot.actuate.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.dubbo.metrics.DubboMetrics;
import org.apache.dubbo.spring.boot.actuate.mertics.DubboMetricsBinder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author songxiaosheng
 */
@Configuration(
    proxyBeanMethods = false
)
@ConditionalOnWebApplication
@ConditionalOnClass({DubboMetrics.class})
public class DubboMetricsAutoConfiguration {
    @Bean
    @ConditionalOnBean({MeterRegistry.class})
    @ConditionalOnMissingBean({DubboMetrics.class, DubboMetricsBinder.class})
    public DubboMetricsBinder tomcatMetricsBinder(MeterRegistry meterRegistry) {
        return new DubboMetricsBinder(meterRegistry);
    }
}
