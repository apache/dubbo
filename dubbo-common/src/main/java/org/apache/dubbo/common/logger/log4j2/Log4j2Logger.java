package org.apache.dubbo.common.logger.log4j2;
import org.apache.dubbo.common.logger.Logger;
import org.apache.logging.log4j.Level;


public class Log4j2Logger implements Logger {
    private final org.apache.logging.log4j.Logger logger;

    public Log4j2Logger(org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void trace(String msg) {
        logger.log(Level.TRACE, msg);
    }

    @Override
    public void trace(Throwable e) {
        logger.log(Level.TRACE, e == null ? null : e.getMessage(), e);
    }


    @Override
    public void trace(String msg, Throwable e) {
        logger.log(Level.TRACE, msg, e);
    }

    @Override
    public void debug(String msg) {
        logger.log(Level.DEBUG, msg);
    }

    @Override
    public void debug(Throwable e) {
        logger.log(Level.DEBUG, e == null ? null : e.getMessage(), e);
    }


    @Override
    public void debug(String msg, Throwable e) {
        logger.log(Level.DEBUG, msg, e);
    }

    @Override
    public void info(String msg) {
        logger.log(Level.INFO, msg);
    }

    @Override
    public void info(Throwable e) {
        logger.log(Level.INFO, e == null ? null : e.getMessage(), e);
    }

    @Override
    public void info(String msg, Throwable e) {
        logger.log(Level.INFO, msg, e);
    }

    @Override
    public void warn(String msg) {
        logger.log(Level.WARN, msg);
    }

    @Override
    public void warn(Throwable e) {
        logger.log(Level.WARN, e == null ? null : e.getMessage(), e);
    }


    @Override
    public void warn(String msg, Throwable e) {
        logger.log(Level.WARN, msg, e);
    }

    @Override
    public void error(String msg) {
        logger.log(Level.ERROR, msg);
    }

    @Override
    public void error(Throwable e) {
        logger.log(Level.ERROR, e == null ? null : e.getMessage(), e);
    }

    @Override
    public void error(String msg, Throwable e) {
        logger.log(Level.ERROR, msg, e);
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
        return logger.isEnabled(Level.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isEnabled(Level.ERROR);
    }
}

