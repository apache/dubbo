package org.apache.dubbo.metrics.collector;

import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metrics.model.ConfigCenterMetric;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_VERSION_KEY;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author guiyi.yuan
 * @date 2/20/23 9:08 AM
 * @description
 */
class ConfigCenterMetricsCollectorTest {

    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;

    @BeforeEach
    public void setup() {
        frameworkModel = FrameworkModel.defaultModel();
        applicationModel = frameworkModel.newApplication();
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");

        applicationModel.getApplicationConfigManager().setApplication(config);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void increase4Initialized() {
        ConfigCenterMetricsCollector collector = new ConfigCenterMetricsCollector();
        collector.setCollectEnabled(true);
        String applicationName = applicationModel.getApplicationName();
        collector.increase4Initialized("key", "group", "nacos", applicationName, 1);
        collector.increase4Initialized("key", "group", "nacos", applicationName, 1);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Assertions.assertTrue(sample instanceof GaugeMetricSample);
            GaugeMetricSample gaugeSample = (GaugeMetricSample) sample;
            Map<String, String> tags = gaugeSample.getTags();
            Supplier<Number> supplier = gaugeSample.getSupplier();

            Assertions.assertEquals(supplier.get().longValue(), 2);
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationName);
        }
    }

    @Test
    void increaseUpdated() {
        ConfigCenterMetricsCollector collector = new ConfigCenterMetricsCollector();
        collector.setCollectEnabled(true);
        String applicationName = applicationModel.getApplicationName();

        ConfigChangedEvent event = new ConfigChangedEvent("key", "group", null, ConfigChangeType.ADDED);
        
        collector.increaseUpdated("nacos", applicationName, event);
        collector.increaseUpdated("nacos", applicationName, event);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Assertions.assertTrue(sample instanceof GaugeMetricSample);
            GaugeMetricSample gaugeSample = (GaugeMetricSample) sample;
            Map<String, String> tags = gaugeSample.getTags();
            Supplier<Number> supplier = gaugeSample.getSupplier();

            Assertions.assertEquals(supplier.get().longValue(), 2);
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationName);
        }
    }
}
