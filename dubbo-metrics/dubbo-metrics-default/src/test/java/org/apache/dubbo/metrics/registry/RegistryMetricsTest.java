package org.apache.dubbo.metrics.registry;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.Mockito.*;


public class RegistryMetricsTest {

    ApplicationModel applicationModel;

    RegistryMetricsCollector collector;

    String REGISTER = "register";

    String REGISTRY_REGISTER = "registry.register";


    @BeforeEach
    void setUp() {
        ApplicationModel applicationModel = getApplicationModel();
        this.collector = getTestCollector(applicationModel);
        this.collector.setCollectEnabled(true);
    }

    @Test
    void testSucceedRegisterRequests() {
        RegistryEvent event = creatEvent();
        collector.onEvent(event);

//        MetricSample sample = getSample(MetricsKey.METRIC_REQUESTS_SUCCEED.getNameByType(REGISTRY_REGISTER), collector.collect());
//        Assertions.assertTrue(sample);


        collector.onEventFinish(event);

        MetricSample sample = getSample(MetricsKey.METRIC_REQUESTS_SUCCEED.getNameByType(REGISTRY_REGISTER), collector.collect());

    }

    @Test
    void testTotalRegisterRequests() {
        MetricsKey.METRIC_REQUESTS.getNameByType("registry.regsiter");
    }

    @Test
    void testRegistryFailed() {
        MetricsKey.REGISTER_METRIC_REQUESTS_FAILED.getNameByType("registry.register");
    }

    @Test
    void testLastResponseTime() {
        MetricsKey.METRIC_RT_LAST.getNameByType(REGISTER);
    }

    @Test
    void testMinResponseTime() {
        MetricsKey.METRIC_RT_MIN.getNameByType(REGISTER);
    }

    @Test
    void testMaxResponseTime() {
        MetricsKey.METRIC_RT_MAX.getNameByType(REGISTER);
    }

    @Test
    void testSumResponseTime() {
        MetricsKey.METRIC_RT_SUM.getNameByType(REGISTER);
    }

    @Test
    void testAvgResponseTime() {

        MetricsKey.METRIC_RT_AVG.getNameByType(REGISTER);
    }

    MetricSample getSample(String name, List<MetricSample> samples) {
        return samples.stream().filter(metricSample -> metricSample.getName().equals(name)).findFirst().orElseThrow(NoSuchElementException::new);
    }

    RegistryEvent creatEvent() {
        RegistryEvent event = RegistryEvent.toRegisterEvent(applicationModel);
        event.setAvailable(true);
        return event;
    }

    ApplicationModel getApplicationModel() {
        return spy(new FrameworkModel().newApplication());
    }

    RegistryMetricsCollector getTestCollector(ApplicationModel applicationModel) {
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
