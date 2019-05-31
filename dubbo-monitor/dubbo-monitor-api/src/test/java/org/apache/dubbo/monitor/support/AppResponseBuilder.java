package org.apache.dubbo.monitor.support;

import org.apache.dubbo.rpc.AppResponse;

import java.util.Map;

public class AppResponseBuilder {
    private Object result;
    private Throwable exception;
    private Map<String, String> attachments;

    private AppResponseBuilder() {
    }

    public AppResponse build() {
        return new AppResponse(this);
    }

    public AppResponseBuilder withResult(Object result) {
        this.result = result;
        return this;
    }

    public AppResponseBuilder withException(Throwable exception) {
        this.exception = exception;
        return this;
    }

    public AppResponseBuilder withAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
        return this;
    }
}
