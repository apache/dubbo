package org.apache.dubbo.registry.retry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.support.FailbackRegistry;

/**
 * FailedUnregisteredTask
 */
public final class FailedUnregisteredTask extends AbstractRetryTask {

    private static final String NAME = "retry unregister";

    public FailedUnregisteredTask(URL url, FailbackRegistry registry) {
        super(url, registry, NAME);
    }

    @Override
    protected void doRetry(URL url, FailbackRegistry registry) {
        registry.doUnregister(url);
        registry.removeFailedUnregisteredTask(url);
    }
}
