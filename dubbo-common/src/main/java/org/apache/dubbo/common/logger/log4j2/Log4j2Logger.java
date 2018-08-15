package org.apache.dubbo.common.logger.log4j2;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.support.FailsafeLogger;

import org.apache.logging.log4j.Level;

public class Log4j2Logger implements Logger {
    private static final String FQCN = FailsafeLogger.class.getName();
    private static org.apache.logging.log4j.Logger logger;

    public Log4j2Logger(org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void trace(String msg) {
        logger.log(Level.TRACE, FQCN,  msg, null);
    }

    @Override
    public void trace(Throwable e) {
        logger.log(Level.TRACE,FQCN,  e == null ? null : e.getMessage(), e);
    }

    @Override
    public void trace(String msg, Throwable e) {
        logger.log(Level.TRACE,FQCN,  msg, e);
    }

    @Override
    public void debug(String msg) {
        logger.log(Level.DEBUG,FQCN,  msg, null);
    }

    @Override
    public void debug(Throwable e) {
        logger.log(Level.DEBUG, FQCN, e == null ? null : e.getMessage(), e);
    }

    @Override
    public void debug(String msg, Throwable e) {
        logger.log(Level.DEBUG, FQCN, msg, e);
    }

    @Override
    public void info(String msg) {
        logger.log(Level.INFO, FQCN, msg, null);
    }

    @Override
    public void info(Throwable e) {
        logger.log(Level.INFO, FQCN, e == null ? null : e.getMessage(), e);
    }

    @Override
    public void info(String msg, Throwable e) {
        logger.log(Level.INFO, FQCN, msg, e);
    }

    @Override
    public void warn(String msg) {
        logger.log(Level.WARN, FQCN, msg, null);
    }

    @Override
    public void warn(Throwable e) {
        logger.log(Level.WARN, FQCN, e == null ? null : e.getMessage(), e);
    }

    @Override
    public void warn(String msg, Throwable e) {
        logger.log(Level.WARN, FQCN, msg, e);
    }

    @Override
    public void error(String msg) {
        logger.log(Level.ERROR, FQCN, msg, null);
    }

    @Override
    public void error(Throwable e) {
        logger.log(Level.ERROR, FQCN, e == null ? null : e.getMessage(), e);
    }

    @Override
    public void error(String msg, Throwable e) {
        logger.log(Level.ERROR, FQCN, msg, e);
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
