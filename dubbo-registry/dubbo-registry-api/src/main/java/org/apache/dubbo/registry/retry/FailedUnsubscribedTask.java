package org.apache.dubbo.registry.retry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;

/**
 * FailedUnsubscribedTask
 */
public final class FailedUnsubscribedTask extends AbstractRetryTask {

    private static final String NAME = "retry unsubscribe";

    private final NotifyListener listener;

    public FailedUnsubscribedTask(URL url, FailbackRegistry registry, NotifyListener listener) {
        super(url, registry, NAME);
        if (listener == null) {
            throw new IllegalArgumentException();
        }
        this.listener = listener;
    }

    @Override
    protected void doRetry(URL url, FailbackRegistry registry) {
        registry.unsubscribe(url, listener);
        registry.removeFailedUnsubscribedTask(url, listener);
    }
}
