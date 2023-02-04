package org.apache.dubbo.metrics.event;

public class ApplicationEvent extends MetricsEvent{
    private ApplicationEvent.Type type;

    public ApplicationEvent(Object source, ApplicationEvent.Type type) {
        super(source);
        this.type = type;
    }

    public ApplicationEvent.Type getType() {
        return type;
    }

    public void setType(ApplicationEvent.Type type) {
        this.type = type;
    }

}
