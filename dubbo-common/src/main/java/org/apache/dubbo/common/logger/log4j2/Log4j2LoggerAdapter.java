package org.apache.dubbo.common.logger.log4j2;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerAdapter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;

public class Log4j2LoggerAdapter implements LoggerAdapter {
    private File file;
    @Override
    public Logger getLogger(Class<?> key) {
        return new Log4j2Logger(LogManager.getLogger(key));
    }

    @Override
    public Logger getLogger(String key) {
        return new Log4j2Logger(LogManager.getLogger(key));
    }

    @Override
    public org.apache.dubbo.common.logger.Level getLevel() {
        return fromLog4j2Level(LogManager.getRootLogger().getLevel());
    }

    private org.apache.dubbo.common.logger.Level fromLog4j2Level(org.apache.logging.log4j.Level level) {
        if (level == org.apache.logging.log4j.Level.ALL)
            return org.apache.dubbo.common.logger.Level.ALL;
        if (level == org.apache.logging.log4j.Level.TRACE)
            return org.apache.dubbo.common.logger.Level.TRACE;
        if (level == org.apache.logging.log4j.Level.DEBUG)
            return org.apache.dubbo.common.logger.Level.DEBUG;
        if (level == org.apache.logging.log4j.Level.INFO)
            return org.apache.dubbo.common.logger.Level.INFO;
        if (level == org.apache.logging.log4j.Level.WARN)
            return org.apache.dubbo.common.logger.Level.WARN;
        if (level == org.apache.logging.log4j.Level.ERROR)
            return org.apache.dubbo.common.logger.Level.ERROR;
        return org.apache.dubbo.common.logger.Level.OFF;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void setFile(File file) {
    }

    @Override
    public void setLevel(org.apache.dubbo.common.logger.Level level) {
        Configurator.setLevel(LogManager.ROOT_LOGGER_NAME, toLog4j2Level(level));
        return;
    }

    public void setRootLevel(org.apache.dubbo.common.logger.Level level){
        Configurator.setRootLevel(toLog4j2Level(level));
    }

    private Level toLog4j2Level(org.apache.dubbo.common.logger.Level level) {
        if (level == org.apache.dubbo.common.logger.Level.ALL)
            return org.apache.logging.log4j.Level.ALL;
        if (level == org.apache.dubbo.common.logger.Level.TRACE)
            return org.apache.logging.log4j.Level.TRACE;
        if (level == org.apache.dubbo.common.logger.Level.DEBUG)
            return org.apache.logging.log4j.Level.DEBUG;
        if (level == org.apache.dubbo.common.logger.Level.INFO)
            return org.apache.logging.log4j.Level.INFO;
        if (level == org.apache.dubbo.common.logger.Level.WARN)
            return org.apache.logging.log4j.Level.WARN;
        if (level == org.apache.dubbo.common.logger.Level.ERROR)
            return org.apache.logging.log4j.Level.ERROR;
        return org.apache.logging.log4j.Level.OFF;
    }
}
