/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.logger.Level;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

@Plugin(name = "Dubbo", category = "Core", elementType = "appender")
public class DubboAppender extends AbstractAppender {

    public static class Builder extends AbstractOutputStreamAppender.Builder<Builder>
            implements org.apache.logging.log4j.core.util.Builder<DubboAppender> {

        @PluginBuilderAttribute
        private String fileName;

        @PluginBuilderAttribute
        private boolean append = true;

        @PluginBuilderAttribute
        private boolean locking;

        public Builder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder setAppend(boolean append) {
            this.append = append;
            return this;
        }

        public Builder setLocking(boolean locking) {
            this.locking = locking;
            return this;
        }

        @Override
        public DubboAppender build() {
            return new DubboAppender(getName(), buildFileAppender());
        }

        private <B extends FileAppender.Builder<B>> FileAppender buildFileAppender() {
            FileAppender.Builder<B> builder = FileAppender.newBuilder();
            builder.setIgnoreExceptions(isIgnoreExceptions());
            builder.setLayout(getLayout());
            builder.setName(getName() + "-File");
            builder.setConfiguration(getConfiguration());
            builder.setBufferedIo(isBufferedIo());
            builder.setBufferSize(getBufferSize());
            builder.setImmediateFlush(isImmediateFlush());
            builder.withFileName(fileName == null || fileName.isEmpty() ? DEFAULT_FILE_NAME : fileName);
            builder.withAppend(append);
            builder.withLocking(locking);
            return builder.build();
        }
    }

    private static final String DEFAULT_FILE_NAME = "dubbo.log";

    public static boolean available = false;
    public static List<Log> logList = new ArrayList<>();

    private final FileAppender fileAppender;

    public DubboAppender() {
        this("Dubbo", null);
    }

    private DubboAppender(String name, FileAppender fileAppender) {
        super(name, null, null, true, Property.EMPTY_ARRAY);
        this.fileAppender = fileAppender;
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder().asBuilder();
    }

    public static void doStart() {
        available = true;
    }

    public static void doStop() {
        available = false;
    }

    public static void clear() {
        logList.clear();
    }

    @Override
    public void append(LogEvent event) {
        if (fileAppender != null) {
            fileAppender.append(event);
        }
        if (available) {
            logList.add(parseLog(event));
        }
    }

    @Override
    public void initialize() {
        fileAppender.initialize();
        super.initialize();
    }

    @Override
    public void start() {
        fileAppender.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        fileAppender.stop();
    }

    private Log parseLog(LogEvent event) {
        Log log = new Log();
        log.setLogName(event.getLoggerName());
        log.setLogLevel(Level.valueOf(event.getLevel().name()));
        log.setLogThread(event.getThreadName());
        log.setLogMessage(event.getMessage().getFormattedMessage());
        return log;
    }
}
