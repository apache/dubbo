package org.apache.dubbo.metrics.event;

import org.apache.dubbo.metrics.listener.MetricsLifeListener;
import org.apache.dubbo.metrics.listener.MetricsListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class BaseMetricsEventMulticaster implements MetricsEventMulticaster {

    private final List<MetricsListener<?>> listeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void addListener(MetricsListener<?> listener) {
        listeners.add(listener);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void publishEvent(MetricsEvent<?> event) {
        if (event instanceof EmptyEvent) {
            return;
        }
        for (MetricsListener listener : listeners) {
            if (listener.isSupport(event)) {
                listener.onEvent(event);
            }
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void publishFinishEvent(MetricsEvent<?> event) {
        publishTimeEvent(event, metricsLifeListener -> metricsLifeListener.onEventFinish(event));
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void publishErrorEvent(MetricsEvent<?> event) {
        publishTimeEvent(event, metricsLifeListener -> metricsLifeListener.onEventError(event));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void publishTimeEvent(MetricsEvent<?> event, Consumer<MetricsLifeListener> consumer) {
        if (event instanceof EmptyEvent) {
            return;
        }
        if (event instanceof TimeAble) {
            ((TimeAble) event).getTimePair().end();
        }
        for (MetricsListener listener : listeners) {
            if (listener instanceof MetricsLifeListener && listener.isSupport(event)) {
                consumer.accept(((MetricsLifeListener) listener));
            }
        }
    }
}
