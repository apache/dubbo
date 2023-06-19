package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.metrics.collector.CombMetricsCollector;
import org.apache.dubbo.metrics.listener.AbstractMetricsKeyListener;
import org.apache.dubbo.metrics.listener.MetricsApplicationListener;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.registry.RegistryMetricsConstants;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;

import java.util.List;

import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER;

/**
 * Different from the general-purpose listener constructor {@link MetricsApplicationListener} ,
 * it provides registry custom listeners
 */
public class RegistrySpecListener {

    /**
     * Perform auto-increment on the monitored key,
     * Can use a custom listener instead of this generic operation
     *
     * @param metricsKey Monitor key
     * @param collector  Corresponding collector
     */
    public static AbstractMetricsKeyListener onPostOfRegister(MetricsKey metricsKey, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onEvent(metricsKey,
            event -> {
                List<String> registryClusterNames = event.getAttachmentValue(RegistryMetricsConstants.ATTACHMENT_KEY_MULTI_REGISTRY);
                ((RegistryMetricsCollector) collector).incrRegisterNum(metricsKey, registryClusterNames);
            }
        );
    }

    public static AbstractMetricsKeyListener onFinishOfRegister(MetricsKey metricsKey, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onFinish(metricsKey,
            event -> {
                List<String> registryClusterNames = event.getAttachmentValue(RegistryMetricsConstants.ATTACHMENT_KEY_MULTI_REGISTRY);
                ((RegistryMetricsCollector) collector).incrRegisterFinishNum(metricsKey, OP_TYPE_REGISTER.getType(), registryClusterNames, event.getTimePair().calc());
            }
        );
    }

    public static AbstractMetricsKeyListener onErrorOfRegister(MetricsKey metricsKey, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onError(metricsKey,
            event -> {
                List<String> registryClusterNames = event.getAttachmentValue(RegistryMetricsConstants.ATTACHMENT_KEY_MULTI_REGISTRY);
                ((RegistryMetricsCollector) collector).incrRegisterFinishNum(metricsKey, OP_TYPE_REGISTER.getType(), registryClusterNames, event.getTimePair().calc());
            }
        );
    }
}
