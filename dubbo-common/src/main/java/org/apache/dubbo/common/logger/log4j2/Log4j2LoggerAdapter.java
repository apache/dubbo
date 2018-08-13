package org.apache.dubbo.common.logger.log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerAdapter;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import java.io.File;
import java.util.Map;
public class Log4j2LoggerAdapter implements LoggerAdapter {
    private File file;
    public Log4j2LoggerAdapter() {
        try {
            org.apache.logging.log4j.Logger logger = LogManager.getLogger();
            if (logger != null) {
                Map<String, Appender> appenders = ((org.apache.logging.log4j.core.Logger) logger).getAppenders();
                if (appenders != null) {
                    for(Map.Entry<String, Appender> entry: appenders.entrySet()) {
                        if (entry.getValue() instanceof FileAppender) {
                            FileAppender fileAppender = (FileAppender) entry.getValue();
                            String filename = fileAppender.getFileName();
                            file = new File(filename);
                            break;
                        }
                    }
                }
            }
        } catch (Throwable t) {
        }
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
        // log4j2 didn't provide a setLevel method in LogManager.getRootLogger() and in Log4J Core.
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        // use reflection to set RootLogger Level
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(toLog4j2Level(level));
        ctx.updateLoggers();
    }
    @Override
    public File getFile() {
        return file;
    }
    @Override
    public void setFile(File file) {
        this.file = file;
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
}