package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.listener.MetricsLifeListener;

import java.util.function.BiConsumer;

public abstract class RegistryListener implements MetricsLifeListener<RegistryEvent> {

    private final Object enumType;

    public RegistryListener(Object enumType) {
        this.enumType = enumType;
    }

    @Override
    public boolean isSupport(MetricsEvent event) {
        return event.isAvailable() && event.isAssignableFrom(enumType);
    }

    static <T> RegistryListener onEvent(T enumType, BiConsumer<RegistryEvent, T> postFunc) {

        return new RegistryListener(enumType) {
            @Override
            public void onEvent(RegistryEvent event) {
                postFunc.accept(event, enumType);
            }
        };
    }

    static <T> RegistryListener onFinish(T enumType, BiConsumer<RegistryEvent, T> finishFunc) {

        return new RegistryListener(enumType) {
            @Override
            public void onEventFinish(RegistryEvent event) {
                finishFunc.accept(event, enumType);
            }
        };
    }

    static <T> RegistryListener onError(T enumType, BiConsumer<RegistryEvent, T> errorFunc) {

        return new RegistryListener(enumType) {
            @Override
            public void onEventError(RegistryEvent event) {
                errorFunc.accept(event, enumType);
            }
        };
    }
}
