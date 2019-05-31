package org.apache.dubbo.monitor.dubbo;

import org.apache.dubbo.rpc.AppResponse;

import java.util.Map;

public class AppResponseBuilder {
    private Throwable exception;
    private Map<String, String> attachments;
    private AppResponse appResponse;

    private AppResponseBuilder() {
        appResponse = new AppResponse();
    }

    public static AppResponseBuilder create() {
        return new AppResponseBuilder();
    }

    public AppResponseBuilder withException(Throwable exception) {
        this.exception = exception;
        return this;
    }

    public AppResponseBuilder withAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
        return this;
    }

    public AppResponse build() {
        appResponse.setException(this.exception);
        appResponse.setAttachments(this.attachments);
        return this.appResponse;
    }
}
