package org.apache.dubbo.metrics.registry;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;



public class RegistryMetricsTest {


    @Test
    void testRegisterMetricsCount() {

        ApplicationModel applicationModel = getApplicationModel();
        RegistryMetricsCollector collector = getTestCollector(applicationModel);
        collector.setCollectEnabled(true);

        RegistryEvent event = RegistryEvent.toRegisterEvent(applicationModel);
        event.setAvailable(true);
        collector.onEvent(event);

        RegistryEvent errEvent = RegistryEvent.toRegisterEvent(applicationModel);
        errEvent.setAvailable(true);
        collector.onEventError(errEvent);


        List<MetricSample> samples = collector.collect();
        System.out.println(samples);
    }

    @Test
    void testRegisterFailedCount() {

    }

    @Test
    void testRegisterRequestsCount() {

    }

    @Test
    void testFailedRegisterCount() {


    }

    public static MethodMetric getTestMethodMetric() {

        MethodMetric methodMetric = new MethodMetric();
        methodMetric.setApplicationName("TestApp");
        methodMetric.setInterfaceName("TestInterface");
        methodMetric.setMethodName("TestMethod");
        methodMetric.setGroup("TestGroup");
        methodMetric.setVersion("1.0.0");
        methodMetric.setSide("PROVIDER");

        return methodMetric;
    }

    public static ApplicationModel getApplicationModel(){
        return spy(new FrameworkModel().newApplication());
    }

    public static RegistryMetricsCollector getTestCollector(ApplicationModel applicationModel) {
        ApplicationConfig applicationConfig = new ApplicationConfig("TestApp");
        ConfigManager configManager = spy(new ConfigManager(applicationModel));
        MetricsConfig metricsConfig = spy(new MetricsConfig());
        configManager.setApplication(applicationConfig);
        configManager.setMetrics(metricsConfig);

        when(metricsConfig.getAggregation()).thenReturn(new AggregationConfig());
        when(applicationModel.getApplicationConfigManager()).thenReturn(configManager);
        when(applicationModel.NotExistApplicationConfig()).thenReturn(false);
        when(configManager.getApplication()).thenReturn(Optional.of(applicationConfig));

        ScopeBeanFactory beanFactory = mock(ScopeBeanFactory.class);
        when(beanFactory.getBean(DefaultMetricsCollector.class)).thenReturn(new DefaultMetricsCollector());
        when(applicationModel.getBeanFactory()).thenReturn(beanFactory);

        return new RegistryMetricsCollector(applicationModel);
    }

}
