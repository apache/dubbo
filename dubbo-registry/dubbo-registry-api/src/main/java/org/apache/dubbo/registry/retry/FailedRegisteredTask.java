package org.apache.dubbo.registry.retry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.support.FailbackRegistry;

/**
 * FailedRegisteredTask
 */
public final class FailedRegisteredTask extends AbstractRetryTask {

    private static final String NAME = "retry register";

    public FailedRegisteredTask(URL url, FailbackRegistry registry) {
        super(url, registry, NAME);
    }

    @Override
    protected void doRetry(URL url, FailbackRegistry registry) {
        registry.doRegister(url);
    }
}
