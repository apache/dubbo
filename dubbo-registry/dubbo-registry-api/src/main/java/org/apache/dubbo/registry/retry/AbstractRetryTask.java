package org.apache.dubbo.registry.retry;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.support.FailbackRegistry;

import java.util.concurrent.TimeUnit;

/**
 * AbstractRetryTask
 */
public abstract class AbstractRetryTask implements TimerTask {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * url for retry task
     */
    protected final URL url;

    /**
     * registry for this task
     */
    protected final FailbackRegistry registry;

    /**
     * retry period
     */
    private final long retryPeriod;

    /**
     * task name for this task
     */
    private final String taskName;

    AbstractRetryTask(URL url, FailbackRegistry registry, String taskName) {
        if (url == null || registry == null || StringUtils.isBlank(taskName)) {
            throw new IllegalArgumentException();
        }
        this.url = url;
        this.registry = registry;
        this.taskName = taskName;
        this.retryPeriod = url.getParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RETRY_PERIOD);
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

    @Override
    public void run(Timeout timeout) throws Exception {
        if (timeout.isCancelled() || timeout.timer().isStop()) {
            // other thread cancel this timeout or stop the timer.
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info(taskName + " : " + url);
        }
        try {
            doRetry(url, registry);
        } catch (Throwable t) { // Ignore all the exceptions and wait for the next retry
            logger.warn("Failed to " + taskName + url + ", waiting for again, cause:" + t.getMessage(), t);
            // reput this task when catch exception.
            reput(timeout, retryPeriod);
        }
    }

    protected abstract void doRetry(URL url, FailbackRegistry registry);
}
