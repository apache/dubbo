package org.apache.dubbo.common.logger;

/**
 * Log Listener, can registered to an {@link ListenableLogger}.
 */
public interface LogListener {

    void onMessage(String code, String msg);

}
