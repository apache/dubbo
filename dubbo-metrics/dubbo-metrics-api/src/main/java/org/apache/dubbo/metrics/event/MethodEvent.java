package org.apache.dubbo.metrics.event;

public class MethodEvent extends MetricsEvent {
    private String type;

    public MethodEvent(Object source, String type) {
        super(source);
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


}
