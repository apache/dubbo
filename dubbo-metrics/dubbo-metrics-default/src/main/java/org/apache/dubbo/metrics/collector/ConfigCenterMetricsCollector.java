package org.apache.dubbo.metrics.collector;

import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.metrics.model.ConfigCenterMetric;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.metrics.model.MetricsCategory.CONFIGCENTER;

/**
 * @author guiyi.yuan
 * @date 2/8/23 10:06 PM
 * @description Metrics collector for Config-Center
 */
public class ConfigCenterMetricsCollector extends DefaultMetricsCollector {

    private final Map<ConfigCenterMetric, AtomicLong> updatedMetrics = new ConcurrentHashMap<>();

    public ConfigCenterMetricsCollector() {
    }

    public void increase4Initialized(String key, String group, String protocol, String applicationName, int count) {
        if (count <= 0) {
            return;
        }
        ConfigCenterMetric metric = new ConfigCenterMetric(applicationName, key, group, protocol, ConfigChangeType.ADDED.name());
        AtomicLong aLong = updatedMetrics.computeIfAbsent(metric, k -> new AtomicLong(0L));
        aLong.addAndGet(count);
    }

    public void increaseUpdated(String protocol, String applicationName, ConfigChangedEvent event) {
        ConfigCenterMetric metric = new ConfigCenterMetric(applicationName, event.getKey(), event.getGroup(), protocol, event.getChangeType().name());
        AtomicLong count = updatedMetrics.computeIfAbsent(metric, k -> new AtomicLong(0L));
        count.incrementAndGet();
    }

    @Override
    public List<MetricSample> collect() {
        // Add metrics to reporter
        List<MetricSample> list = new ArrayList<>();
        collect(list);
        return list;
    }

    private void collect(List<MetricSample> list) {
        updatedMetrics.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.CONFIGCENTER_METRIC_TOTAL, k.getTags(), CONFIGCENTER, v::get)));
    }

}
