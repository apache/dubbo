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
            RegistryEvent event = applicationRegister();
            if (i % 2 == 0) {
                eventSuccess(event);
            } else {
                eventFailed(event);
            }
        }
        List<MetricSample> samples = collector.collect();

        GaugeMetricSample<?> succeedRequests = getSample(MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED.getName(), samples);
        GaugeMetricSample<?> failedRequests = getSample(MetricsKey.REGISTER_METRIC_REQUESTS_FAILED.getName(), samples);
        GaugeMetricSample<?> totalRequests = getSample(MetricsKey.REGISTER_METRIC_REQUESTS.getName(), samples);

        Assertions.assertEquals(5L, succeedRequests.applyAsLong());
        Assertions.assertEquals(5L, failedRequests.applyAsLong());
        Assertions.assertEquals(10L, totalRequests.applyAsLong());
    }

    @Test
    void testLastResponseTime() {
        long waitTime = 2000;

        RegistryEvent event = applicationRegister();
        await(waitTime);
        eventSuccess(event);

        GaugeMetricSample<?> sample = getSample(MetricsKey.METRIC_RT_LAST.getNameByType(REGISTER), collector.collect());
        // 20% deviation is allowed
        Assertions.assertTrue(considerEquals(waitTime, sample.applyAsLong(), 0.2));

        RegistryEvent event1 = applicationRegister();
        await(waitTime / 2);
        eventSuccess(event1);

        sample = getSample(MetricsKey.METRIC_RT_LAST.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals((double) waitTime / 2, sample.applyAsLong(), 0.2));

        RegistryEvent event2 = applicationRegister();
        await(waitTime);
        eventFailed(event2);

        sample = getSample(MetricsKey.METRIC_RT_LAST.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals((double) waitTime, sample.applyAsLong(), 0.2));
    }

    @Test
    void testMinResponseTime() throws InterruptedException {
        long waitTime = 2000L;

        RegistryEvent event = applicationRegister();
        await(waitTime);
        eventSuccess(event);

        RegistryEvent event1 = applicationRegister();
        await(waitTime);

        RegistryEvent event2 = applicationRegister();
        await(waitTime);

        eventSuccess(event1);
        eventSuccess(event2);

        GaugeMetricSample<?> sample = getSample(MetricsKey.METRIC_RT_MIN.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals(waitTime, sample.applyAsLong(), 0.2));

        RegistryEvent event3 = applicationRegister();
        Thread.sleep(waitTime / 2);
        eventSuccess(event3);

        sample = getSample(MetricsKey.METRIC_RT_MIN.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals((double) waitTime / 2, sample.applyAsLong(), 0.2));
    }

    @Test
    void testMaxResponseTime() {
        long waitTime = 1000L;

        RegistryEvent event = applicationRegister();
        await(waitTime);
        eventSuccess(event);

        GaugeMetricSample<?> sample = getSample(MetricsKey.METRIC_RT_MAX.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals(waitTime, sample.applyAsLong(), 0.2));

        RegistryEvent event1 = applicationRegister();
        await(waitTime * 2);
        eventSuccess(event1);

        sample = getSample(MetricsKey.METRIC_RT_MAX.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals(waitTime * 2, sample.applyAsLong(), 0.2));

        sample = getSample(MetricsKey.METRIC_RT_MAX.getNameByType(REGISTER), collector.collect());
        RegistryEvent event2 = applicationRegister();
        eventSuccess(event2);
        Assertions.assertTrue(considerEquals(waitTime * 2, sample.applyAsLong(), 0.2));
    }

    @Test
    void testSumResponseTime() {
        long waitTime = 1000;

        RegistryEvent event = applicationRegister();
        RegistryEvent event1 = applicationRegister();
        RegistryEvent event2 = applicationRegister();

        await(waitTime);

        eventSuccess(event);
        eventFailed(event1);

        GaugeMetricSample<?> sample = getSample(MetricsKey.METRIC_RT_SUM.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals(waitTime * 2, sample.applyAsLong(), 0.2));

        await(waitTime);
        eventSuccess(event2);

        sample = getSample(MetricsKey.METRIC_RT_SUM.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals(waitTime * 4, sample.applyAsLong(), 0.2));
    }

    @Test
    void testAvgResponseTime() {
        long waitTime = 1000;

        RegistryEvent event = applicationRegister();
        RegistryEvent event1 = applicationRegister();
        RegistryEvent event2 = applicationRegister();

        await(waitTime);

        eventSuccess(event);
        eventFailed(event1);

        GaugeMetricSample<?> sample = getSample(MetricsKey.METRIC_RT_AVG.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals(waitTime, sample.applyAsLong(), 0.2));

        await(waitTime);
        eventSuccess(event2);

        sample = getSample(MetricsKey.METRIC_RT_AVG.getNameByType(REGISTER), collector.collect());
        Assertions.assertTrue(considerEquals((double) waitTime * 4 / 3, sample.applyAsLong(), 0.2));
    }

    @Test
    void testServiceRegisterCount() {

        for (int i = 0; i < 10; i++) {
            RegistryEvent event = serviceRegister();
            if (i % 2 == 0) {
                eventSuccess(event);
            } else {
                eventFailed(event);
            }
        }
        List<MetricSample> samples = collector.collect();

        GaugeMetricSample<?> succeedRequests = getSample(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED.getName(), samples);
        GaugeMetricSample<?> failedRequests = getSample(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED.getName(), samples);
        GaugeMetricSample<?> totalRequests = getSample(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS.getName(), samples);

        Assertions.assertEquals(5L, succeedRequests.applyAsLong());
        Assertions.assertEquals(5L, failedRequests.applyAsLong());
        Assertions.assertEquals(10L, totalRequests.applyAsLong());

    }

    @Test
    void testServiceSubscribeCount() {

        for (int i = 0; i < 10; i++) {
            RegistryEvent event = serviceSubscribe();
            if (i % 2 == 0) {
                eventSuccess(event);
            } else {
                eventFailed(event);
            }
        }
        List<MetricSample> samples = collector.collect();

        GaugeMetricSample<?> succeedRequests = getSample(MetricsKey.SUBSCRIBE_METRIC_NUM_SUCCEED.getName(), samples);
        GaugeMetricSample<?> failedRequests = getSample(MetricsKey.SUBSCRIBE_METRIC_NUM_FAILED.getName(), samples);
        GaugeMetricSample<?> totalRequests = getSample(MetricsKey.SUBSCRIBE_METRIC_NUM.getName(), samples);

        Assertions.assertEquals(5L, succeedRequests.applyAsLong());
        Assertions.assertEquals(5L, failedRequests.applyAsLong());
        Assertions.assertEquals(10L, totalRequests.applyAsLong());
    }


    GaugeMetricSample<?> getSample(String name, List<MetricSample> samples) {
        return (GaugeMetricSample<?>) samples.stream().filter(metricSample -> metricSample.getName().equals(name)).findFirst().orElseThrow(NoSuchElementException::new);
    }

    RegistryEvent applicationRegister() {
        RegistryEvent event = registerEvent();
        collector.onEvent(event);
        return event;
    }

    RegistryEvent serviceRegister() {
        RegistryEvent event = rsEvent();
        collector.onEvent(event);
        return event;
    }

    RegistryEvent serviceSubscribe() {
        RegistryEvent event = subscribeEvent();
        collector.onEvent(event);
        return event;
    }

    boolean considerEquals(double expected, double trueValue, double allowedErrorRatio) {
        return Math.abs(1 - expected / trueValue) <= allowedErrorRatio;
    }

    void eventSuccess(RegistryEvent event) {
        collector.onEventFinish(event);
    }

    void eventFailed(RegistryEvent event) {
        collector.onEventError(event);
    }

    RegistryEvent registerEvent() {
        RegistryEvent event = RegistryEvent.toRegisterEvent(applicationModel);
        event.setAvailable(true);
        return event;
    }

    RegistryEvent rsEvent() {
        RegistryEvent event = RegistryEvent.toRsEvent(applicationModel, "TestServiceInterface1", 1);
        event.setAvailable(true);
        return event;
    }

    RegistryEvent subscribeEvent() {
        RegistryEvent event = RegistryEvent.toSubscribeEvent(applicationModel);
        event.setAvailable(true);
        return event;
    }

    ApplicationModel getApplicationModel() {
        return spy(new FrameworkModel().newApplication());
    }

    void await(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
