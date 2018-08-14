package org.apache.dubbo.common.logger.log4j2;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.support.FailsafeLogger;
import org.apache.log4j.Level;

public class Log4j2Logger implements Logger {

    private static final String FQCN = FailsafeLogger.class.getName();

    private org.apache.logging.log4j.Logger logger;

    public Log4j2Logger(org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void trace(String msg) {
        logger.trace(FQCN, Level.TRACE, msg, null);
    }

    @Override
    public void trace(Throwable e) {
        logger.trace(FQCN, e == null ? null : e.getMessage(), e);
    }

    @Override
    public void trace(String msg, Throwable e) {
        logger.trace(FQCN, msg, e);
    }

    @Override
    public void debug(String msg) {
        logger.debug(FQCN, msg, null);
    }

    @Override
    public void debug(Throwable e) {
        logger.debug(FQCN, e == null ? null : e.getMessage(), e);
    }

    @Override
    public void debug(String msg, Throwable e) {
        logger.debug(FQCN, msg, e);
    }

    @Override
    public void info(String msg) {
        logger.info(FQCN, msg, null);
    }

    @Override
    public void info(Throwable e) {
        logger.info(FQCN, e == null ? null : e.getMessage(), e);
    }

    @Override
    public void info(String msg, Throwable e) {
        logger.info(FQCN, msg, e);
    }

    @Override
    public void warn(String msg) {
        logger.warn(FQCN, msg, null);
    }

    @Override
    public void warn(Throwable e) {
        logger.warn(FQCN, e == null ? null : e.getMessage(), e);
    }

    @Override
    public void warn(String msg, Throwable e) {
        logger.warn(FQCN, msg, e);
    }

    @Override
    public void error(String msg) {
        logger.error(FQCN, msg, null);
    }

    @Override
    public void error(Throwable e) {
        logger.error(FQCN, e == null ? null : e.getMessage(), e);
    }

    @Override
    public void error(String msg, Throwable e) {
        logger.error(FQCN, msg, e);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

}
