package org.apache.dubbo.common.logger.log4j2;

import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

public class Log4j2LoggerAdapter implements LoggerAdapter {

    private File file;

    public Log4j2LoggerAdapter() {
        try {
            org.apache.logging.log4j.Logger logger = LogManager.getRootLogger();
            org.apache.logging.log4j.core.Logger loggerImpl = (org.apache.logging.log4j.core.Logger) logger;
            if (logger != null) {
                Map<String, Appender> appenderMap = loggerImpl.getAppenders();
                Iterator iter = appenderMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    Appender appender = (Appender) entry.getValue();
                    if(appender instanceof FileAppender){
                        FileAppender fileAppender = (FileAppender) appender;
                        String filename = fileAppender.getFileName();
                        file = new File(filename);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
        }
    }

    private static org.apache.logging.log4j.Level toLog4j2Level(Level level) {
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

    private static Level fromLog4j2Level(org.apache.logging.log4j.Level level) {
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
        return fromLog4j2Level(LogManager.getRootLogger().getLevel());
    }

    @Override
    public void setLevel(Level level) {
        Configurator.setRootLevel(toLog4j2Level(level));
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void setFile(File file) {
    }

}
