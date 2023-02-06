package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.listener.MetricsLifeListener;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.function.BiConsumer;

public class RegisterListener implements MetricsLifeListener<RegistryRegisterEvent> {

    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    @Override
    public boolean isSupport(MetricsEvent<?> event) {
        return event instanceof RegistryRegisterEvent;
    }

    @Override
    public void onEvent(RegistryRegisterEvent event) {
        handleIncrementEvent(event, RegistryRegisterEvent.Type.TOTAL);
    }

    @Override
    public void onEventFinish(RegistryRegisterEvent event) {
        handleIncrementEvent(event, RegistryRegisterEvent.Type.SUCCEED);
        handleTimeEvent(event);
    }

    @Override
    public void onEventError(RegistryRegisterEvent event) {
        handleIncrementEvent(event, RegistryRegisterEvent.Type.FAILED);
        handleTimeEvent(event);
    }

    public void handleIncrementEvent(RegistryRegisterEvent event, RegistryRegisterEvent.Type type) {
        handleEvent(event, (applicationModel, collector) -> collector.increment(type, applicationModel.getApplicationName()));
    }

    public void handleTimeEvent(RegistryRegisterEvent event) {
        handleEvent(event, (applicationModel, collector) -> collector.addRT(applicationModel.getApplicationName(), event.getTimePair().calc()));
    }

    public void handleEvent(RegistryRegisterEvent event, BiConsumer<ApplicationModel, RegistryMetricsCollector> consumer) {
        ApplicationModel applicationModel = event.getSource();
        RegistryMetricsCollector collector = applicationModel.getBeanFactory().getBean(RegistryMetricsCollector.class);
        if (collector == null) {
            logger.error("RegisterListener invoked but no collector found");
            return;
        }
        if (!collector.isCollectEnabled()) {
            return;
        }
        consumer.accept(applicationModel, collector);

    }
}
