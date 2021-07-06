package org.apache.dubbo.config.spring.context.event;

import org.apache.dubbo.config.spring.context.DubboConfigBeanInitializer;
import org.springframework.context.ApplicationEvent;

/**
 * An {@link ApplicationEvent} after Dubbo service/reference annotation has been processed.
 * <p />
 * NOTE: This event is used to trigger init {@link DubboConfigBeanInitializer}
 */
public class DubboAnnotationInitedEvent extends ApplicationEvent {
    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with
     *               which the event is associated (never {@code null})
     */
    public DubboAnnotationInitedEvent(Object source) {
        super(source);
    }
}
