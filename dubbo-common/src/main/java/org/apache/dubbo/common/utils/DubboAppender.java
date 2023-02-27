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

import org.apache.log4j.FileAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.List;

public class DubboAppender extends FileAppender {

    private static final String DEFAULT_FILE_NAME = "dubbo.log";

    public DubboAppender() {
        super();
        setFile(DEFAULT_FILE_NAME);
    }

    public static boolean available = false;

    public static List<Log> logList = new ArrayList<>();

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
    public void append(LoggingEvent event) {
        super.append(event);
        if (available) {
            Log temp = parseLog(event);
            logList.add(temp);
        }
    }

    private Log parseLog(LoggingEvent event) {
        Log log = new Log();
        log.setLogName(event.getLogger().getName());
        log.setLogLevel(event.getLevel());
        log.setLogThread(event.getThreadName());
        log.setLogMessage(event.getMessage().toString());
        return log;
    }

}
