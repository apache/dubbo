package org.apache.dubbo.metrics.data;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.ServiceKeyMetric;
import org.apache.dubbo.metrics.model.container.LongContainer;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ApplicationStatComposite {

    public Map<MetricsKey, Map<String, AtomicLong>> applicationNumStats = new ConcurrentHashMap<>();

    public void init(List<MetricsKey> appKeys) {
        if (CollectionUtils.isEmpty(appKeys)) {
            return;
        }
        appKeys.forEach(appKey -> applicationNumStats.put(appKey, new ConcurrentHashMap<>()));
    }

    public void incrementSize(MetricsKey metricsKey, String applicationName, int size) {
        if (!applicationNumStats.containsKey(metricsKey)) {
            return;
        }
        applicationNumStats.get(metricsKey).computeIfAbsent(applicationName, k -> new AtomicLong(0L)).getAndAdd(size);
    }

    public void setApplicationKey(MetricsKey metricsKey, String applicationName, int num) {
        if (!applicationNumStats.containsKey(metricsKey)) {
            return;
        }
        applicationNumStats.get(metricsKey).computeIfAbsent(applicationName, k -> new AtomicLong(0L)).set(num);
    }


    @SuppressWarnings({"rawtypes"})
    public List<GaugeMetricSample> export(MetricsCategory category) {
        List<GaugeMetricSample> list = new ArrayList<>();
        for (MetricsKey type : applicationNumStats.keySet()) {
            Map<String, AtomicLong> stringAtomicLongMap = applicationNumStats.get(type);
            for (String applicationName : stringAtomicLongMap.keySet()) {
                list.add(convertToSample(applicationName, type, category, stringAtomicLongMap.get(applicationName)));
            }
        }
        return list;
    }

    @SuppressWarnings({"rawtypes"})
    private GaugeMetricSample convertToSample(String applicationName, MetricsKey type, MetricsCategory category, AtomicLong targetNumber) {
        return new GaugeMetricSample<>(type, MetricsSupport.applicationTags(applicationName), category, targetNumber, AtomicLong::get);
    }
}
