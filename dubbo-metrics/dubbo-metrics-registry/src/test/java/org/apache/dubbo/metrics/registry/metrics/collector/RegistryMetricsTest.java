package org.apache.dubbo.metrics.registry.metrics.collector;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


public class RegistryMetricsTest {

    ApplicationModel applicationModel;

    RegistryMetricsCollector collector;

    String REGISTER = "register";

    @BeforeEach
    void setUp() {
        this.applicationModel = getApplicationModel();
        this.collector = getTestCollector(this.applicationModel);
        this.collector.setCollectEnabled(true);
    }

    @Test
    void testRegisterRequestsCount() {

        for (int i = 0; i < 10; i++) {
            RegistryEvent event = serviceRegister();
            collector.onEvent(event);
            if (i % 2 == 0) {
                serviceRegisterSuccess(event);
            } else {
                serviceRegisterFailed(event);
            }
        }
        List<MetricSample> samples = collector.collect();

        GaugeMetricSample<?> succeedRequests = (GaugeMetricSample<?>) getSample(MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED.getName(), samples);
        GaugeMetricSample<?> failedRequests = (GaugeMetricSample<?>) getSample(MetricsKey.REGISTER_METRIC_REQUESTS_FAILED.getName(), samples);
        GaugeMetricSample<?> totalRequests = (GaugeMetricSample<?>) getSample(MetricsKey.REGISTER_METRIC_REQUESTS.getName(), samples);
        Assertions.assertEquals(5L, succeedRequests.applyAsLong());
        Assertions.assertEquals(5L, failedRequests.applyAsLong());
        Assertions.assertEquals(10L, totalRequests.applyAsLong());
    }

    @Test
    void testLastResponseTime() throws InterruptedException {
        long waitTime = 2000;

        RegistryEvent event = serviceRegister();

        Thread.sleep(waitTime);

        serviceRegisterSuccess(event);

        GaugeMetricSample<?> sample = (GaugeMetricSample<?>) getSample(MetricsKey.METRIC_RT_LAST.getNameByType(REGISTER), collector.collect());

        // 20% error is allowed
        Assertions.assertTrue(considerEquals(waitTime,sample.applyAsLong(),0.2));
    }

    @Test
    void testMinResponseTime() throws InterruptedException {
        long waitTime = 2000L;

        RegistryEvent event = serviceRegister();
        Thread.sleep(waitTime);
        serviceRegisterSuccess(event);

        RegistryEvent event1 = serviceRegister();
        Thread.sleep(waitTime);

        RegistryEvent event2 = serviceRegister();
        Thread.sleep(waitTime);

        serviceRegisterSuccess(event1);
        serviceRegisterSuccess(event2);

        GaugeMetricSample<?> sample = (GaugeMetricSample<?>) getSample(MetricsKey.METRIC_RT_MIN.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals(waitTime,sample.applyAsLong(),0.2));
        System.out.println(sample.applyAsLong());

        RegistryEvent event3 = serviceRegister();
        Thread.sleep(waitTime/2);
        serviceRegisterSuccess(event3);

        sample = (GaugeMetricSample<?>) getSample(MetricsKey.METRIC_RT_MIN.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals((double) waitTime /2,sample.applyAsLong(),0.2));
        System.out.println(sample.applyAsLong());
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


    RegistryEvent serviceRegister() {
        RegistryEvent event = creatEvent();
        collector.onEvent(event);
        return event;
    }

    boolean considerEquals(double expected, double trueValue, double allowedErrorRatio) {
        return Math.abs(1 - expected / trueValue) <= allowedErrorRatio;
    }

    void serviceRegisterSuccess(RegistryEvent event) {
        collector.onEventFinish(event);
    }

    void serviceRegisterFailed(RegistryEvent event) {
        collector.onEventError(event);
    }

    RegistryEvent creatEvent() {
        RegistryEvent event = RegistryEvent.toRegisterEvent(applicationModel);
        event.setAvailable(true);
        return event;
    }

    ApplicationModel getApplicationModel() {
        ApplicationModel appModel = spy(new FrameworkModel().newApplication());
        return appModel;
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

        return new RegistryMetricsCollector(applicationModel);
    }

}
