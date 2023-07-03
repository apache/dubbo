package org.apache.dubbo.metrics.collector.sample;

import org.apache.dubbo.common.logger.LogListener;
import org.apache.dubbo.common.logger.support.FailsafeErrorTypeAwareLogger;
import org.apache.dubbo.metrics.model.key.MetricsKey;

/**
 * Listen the log of all {@link FailsafeErrorTypeAwareLogger} instances, and add error code count to {@link ErrorCodeSampler}.
 */
public class ErrorCodeMetricsListenRegister implements LogListener {

    private final ErrorCodeSampler errorCodeSampler;

    public ErrorCodeMetricsListenRegister(ErrorCodeSampler errorCodeSampler){
        FailsafeErrorTypeAwareLogger.registerGlobalListen(this);
        this.errorCodeSampler = errorCodeSampler;
        this.errorCodeSampler.addMetricName(MetricsKey.ERROR_CODE_COUNT.getName());
    }

    @Override
    public void onMessage(String code, String msg) {
        errorCodeSampler.inc(code, MetricsKey.ERROR_CODE_COUNT.getName());
    }
}
