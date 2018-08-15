package org.apache.dubbo.common.logger.log4j2;
import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerAdapter;
import org.apache.logging.log4j.core.Appender;
import org.apache.log4j.FileAppender;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.util.Enumeration;

public class Log4j2LoggerAdapter implements LoggerAdapter {

    private File file;

    @SuppressWarnings("unchecked")
    public Log4j2LoggerAdapter() {
        try {
            org.apache.logging.log4j.Logger logger = LogManager.getLogger();
            if (logger != null) {
                Enumeration<Appender> appenders = (Enumeration<Appender>) ((org.apache.logging.log4j.core.Logger)logger).getAppenders().values();
                if (appenders != null) {
                    while (appenders.hasMoreElements()) {
                        Appender appender = appenders.nextElement();
                        if (appender instanceof FileAppender) {
                            FileAppender fileAppender = (FileAppender) appender;
                            String filename = fileAppender.getFile();
                            file = new File(filename);
                            break;
                        }
                    }
                }
            }
        } catch (Throwable t) {
        }
    }

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
        org.apache.logging.log4j.core.config.Configurator.setRootLevel(toLog4jLevel(level));
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void setFile(File file) {

    }
}

