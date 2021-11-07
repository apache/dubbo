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
package org.apache.dubbo.remoting.http.jetty;

import org.apache.dubbo.common.logger.LoggerFactory;
import org.eclipse.jetty.util.log.AbstractLogger;
import org.eclipse.jetty.util.log.Logger;

/**
 * logger adapter for jetty
 */
public class JettyLoggerAdapter extends AbstractLogger {
    protected String name;
    private final org.apache.dubbo.common.logger.Logger logger;

    public JettyLoggerAdapter(){
        this("org.apache.dubbo.remoting.http.jetty");
    }

    public JettyLoggerAdapter(Class<?> clazz){
        this(clazz.getName());
    }

    public JettyLoggerAdapter(String name) {
        this.name = name;
        this.logger = LoggerFactory.getLogger(name);
    }

    @Override
    protected Logger newLogger(String name) {
        return new JettyLoggerAdapter(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void warn(String msg, Object... objects) {
        if (logger.isWarnEnabled()){
            logger.warn(this.format(msg, objects));
        }
    }

    @Override
    public void warn(Throwable throwable) {
        if (logger.isWarnEnabled()){
            logger.warn(throwable);
        }
    }

    @Override
    public void warn(String msg, Throwable throwable) {
        if (logger.isWarnEnabled()){
            logger.warn(msg, throwable);
        }
    }

    @Override
    public void info(String msg, Object... objects) {
        if (logger.isInfoEnabled()){
            logger.info(this.format(msg, objects));
        }
    }

    @Override
    public void info(Throwable throwable) {
        if (logger.isInfoEnabled()){
            logger.info(throwable);
        }
    }

    @Override
    public void info(String msg, Throwable throwable) {
        if (logger.isInfoEnabled()){
            logger.info(msg, throwable);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void setDebugEnabled(boolean enabled) {
        logger.warn("setDebugEnabled not implemented");
    }

    @Override
    public void debug(String msg, Object... objects) {
        if (logger.isDebugEnabled()){
            logger.debug(this.format(msg, objects));
        }
    }

    @Override
    public void debug(Throwable throwable) {
        if (logger.isDebugEnabled()){
            logger.debug(throwable);
        }
    }

    @Override
    public void debug(String msg, Throwable throwable) {
        if (logger.isDebugEnabled()){
            logger.debug(msg, throwable);
        }
    }

    @Override
    public void ignore(Throwable throwable) {
        if (logger.isDebugEnabled()){
            logger.debug("IGNORED EXCEPTION ", throwable);
        }
    }

    private String format(String msg, Object... args) {
        msg = String.valueOf(msg);
        String braces = "{}";
        StringBuilder builder = new StringBuilder();
        int start = 0;
        Object[] var6 = args;
        int var7 = args.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            Object arg = var6[var8];
            int bracesIndex = msg.indexOf(braces, start);
            if (bracesIndex < 0) {
                builder.append(msg.substring(start));
                builder.append(" ");
                builder.append(arg);
                start = msg.length();
            } else {
                builder.append(msg, start, bracesIndex);
                builder.append(arg);
                start = bracesIndex + braces.length();
            }
        }

        builder.append(msg.substring(start));
        return builder.toString();
    }
}
