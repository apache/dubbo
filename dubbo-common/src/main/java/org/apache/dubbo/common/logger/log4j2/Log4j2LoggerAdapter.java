package org.apache.dubbo.common.logger.log4j2;

import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;

/**
 * log4j version 2.10.0
 */
public class Log4j2LoggerAdapter implements LoggerAdapter {


    //log4j2 do not need to assign file, put log4j2.xml into resources
    @Deprecated
    private File file;

    private static org.apache.logging.log4j.Level toLog4jLevel(Level level) {
        if (level == Level.ALL)
            return org.apache.logging.log4j.Level.ALL;
        if (level == Level.TRACE)
            return org.apache.logging.log4j.Level.TRACE;
        if (level == Level.DEBUG)
            return org.apache.logging.log4j.Level.DEBUG;
        if (level == Level.INFO)
            return org.apache.logging.log4j.Level.INFO;
        if (level == Level.WARN)
            return org.apache.logging.log4j.Level.WARN;
        if (level == Level.ERROR)
            return org.apache.logging.log4j.Level.ERROR;
        // if (level == Level.OFF)
        return org.apache.logging.log4j.Level.OFF;
    }

    private static Level fromLog4jLevel(org.apache.logging.log4j.Level level) {
        if (level == org.apache.logging.log4j.Level.ALL)
            return Level.ALL;
        if (level == org.apache.logging.log4j.Level.TRACE)
            return Level.TRACE;
        if (level == org.apache.logging.log4j.Level.DEBUG)
            return Level.DEBUG;
        if (level == org.apache.logging.log4j.Level.INFO)
            return Level.INFO;
        if (level == org.apache.logging.log4j.Level.WARN)
            return Level.WARN;
        if (level == org.apache.logging.log4j.Level.ERROR)
            return Level.ERROR;
        // if (level == org.apache.log4j.Level.OFF)
        return Level.OFF;
    }

    @Override
    public Logger getLogger(Class<?> key) {
        return new Log4j2Logger(LogManager.getLogger(key));
    }

    @Override
    public Logger getLogger(String key) {
        return new Log4j2Logger(LogManager.getLogger(key));
    }

    @Override
    public Level getLevel() {
        return fromLog4jLevel(LogManager.getRootLogger().getLevel());
    }

    @Override
    public void setLevel(Level level) {
        Configurator.setRootLevel(toLog4jLevel(level));
    }

    @Override
    @Deprecated
    public File getFile() {
        return file;
    }

    @Override
    @Deprecated
    public void setFile(File file) {

    }
}
