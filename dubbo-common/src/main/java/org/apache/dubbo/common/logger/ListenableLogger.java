package org.apache.dubbo.common.logger;

/**
 *  Loggers that can register to listen to log messages.
 */
public interface ListenableLogger extends ErrorTypeAwareLogger{

    /**
     * Register a listener to this logger，and get notified when a log happens.
     *
     * @param listener log listener
     */
    void registerListen(LogListener listener);

}
