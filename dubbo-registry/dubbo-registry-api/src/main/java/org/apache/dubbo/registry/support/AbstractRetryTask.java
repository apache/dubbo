package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;

import java.util.concurrent.TimeUnit;

/**
 * AbstractRetryTask
 */
public abstract class AbstractRetryTask implements TimerTask {

    /**
     * url for retry task
     */
    private final URL url;

    public AbstractRetryTask(URL url) {
        this.url = url;
    }

    private void reput(Timeout timeout, Long tick) {
        if (timeout == null || tick == null) {
            throw new IllegalArgumentException();
        }

        Timer timer = timeout.timer();
        if (timer.isStop() || timeout.isCancelled()) {
            return;
        }

        timer.newTimeout(timeout.task(), tick, TimeUnit.MILLISECONDS);
    }
}
