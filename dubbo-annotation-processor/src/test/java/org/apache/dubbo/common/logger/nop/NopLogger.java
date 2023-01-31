package org.apache.dubbo.common.logger.nop;

import org.apache.dubbo.common.logger.Logger;

/**
 * No-op logger implementation.
 */
public class NopLogger implements Logger {
    private NopLogger() { }

    public static NopLogger INSTANCE = new NopLogger();

    @Override
    public void warn(String msg) {
    }
}
