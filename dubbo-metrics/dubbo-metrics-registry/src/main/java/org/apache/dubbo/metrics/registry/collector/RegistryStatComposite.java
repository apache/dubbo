package org.apache.dubbo.metrics.registry.collector;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.RegistryMetricsConstants;
import org.apache.dubbo.metrics.report.AbstractMetricsExport;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.metrics.MetricsConstants.SELF_INCREMENT_SIZE;

public class RegistryStatComposite extends AbstractMetricsExport {

    private final Map<MetricsKey, Map<RegisterKeyMetric, AtomicLong>> numStats = new ConcurrentHashMap<>();

    public RegistryStatComposite(ApplicationModel applicationModel) {
        super(applicationModel);
        init(RegistryMetricsConstants.REGISTER_LEVEL_KEYS);
    }

    public void init(List<MetricsKey> appKeys) {
        if (CollectionUtils.isEmpty(appKeys)) {
            return;
        }
        appKeys.forEach(appKey -> numStats.put(appKey, new ConcurrentHashMap<>()));
    }

    @Override
    public List<MetricSample> export(MetricsCategory category) {
        List<MetricSample> list = new ArrayList<>();
        for (MetricsKey metricsKey : numStats.keySet()) {
            Map<RegisterKeyMetric, AtomicLong> stringAtomicLongMap = numStats.get(metricsKey);
            for (RegisterKeyMetric registerKeyMetric : stringAtomicLongMap.keySet()) {
                list.add(new GaugeMetricSample<>(metricsKey, registerKeyMetric.getTags(), category, stringAtomicLongMap, value -> value.get(registerKeyMetric).get()));
            }
        }
        return list;
    }

    public void incrRegisterNum(MetricsKey metricsKey, String name) {
        if (!numStats.containsKey(metricsKey)) {
            return;
        }
        numStats.get(metricsKey).computeIfAbsent(new RegisterKeyMetric(getApplicationModel(), name), k -> new AtomicLong(0L)).getAndAdd(SELF_INCREMENT_SIZE);
    }
}
