/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.utils;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

import org.apache.log4j.Level;

import java.util.Iterator;
import java.util.List;

/**
 * @author tony.chenl
 */
public class LogUtil {

    private static Logger Log = LoggerFactory.getLogger(LogUtil.class);

    public static void start() {
        DubboAppender.doStart();
    }

    public static void stop() {
        DubboAppender.doStop();
    }

    public static boolean checkNoError() {
        if (findLevel(Level.ERROR) == 0) {
            return true;
        } else {
            return false;
        }

    }

    public static int findName(String expectedLogName) {
        int count = 0;
        List<Log> logList = DubboAppender.logList;
        for (int i = 0; i < logList.size(); i++) {
            String logName = logList.get(i).getLogName();
            if (logName.contains(expectedLogName)) count++;
        }
        return count;
    }

    public static int findLevel(Level expectedLevel) {
        int count = 0;
        List<Log> logList = DubboAppender.logList;
        for (int i = 0; i < logList.size(); i++) {
            Level logLevel = logList.get(i).getLogLevel();
            if (logLevel.equals(expectedLevel)) count++;
        }
        return count;
    }

    public static int findLevelWithThreadName(Level expectedLevel, String threadName) {
        int count = 0;
        List<Log> logList = DubboAppender.logList;
        for (int i = 0; i < logList.size(); i++) {
            Log log = logList.get(i);
            if (log.getLogLevel().equals(expectedLevel) && log.getLogThread().equals(threadName))
                count++;
        }
        return count;
    }

    public static int findThread(String expectedThread) {
        int count = 0;
        List<Log> logList = DubboAppender.logList;
        for (int i = 0; i < logList.size(); i++) {
            String logThread = logList.get(i).getLogThread();
            if (logThread.contains(expectedThread)) count++;
        }
        return count;
    }

    public static int findMessage(String expectedMessage) {
        int count = 0;
        List<Log> logList = DubboAppender.logList;
        for (int i = 0; i < logList.size(); i++) {
            String logMessage = logList.get(i).getLogMessage();
            if (logMessage.contains(expectedMessage)) count++;
        }
        return count;
    }

    public static int findMessage(Level expectedLevel, String expectedMessage) {
        int count = 0;
        List<Log> logList = DubboAppender.logList;
        for (int i = 0; i < logList.size(); i++) {
            Level logLevel = logList.get(i).getLogLevel();
            if (logLevel.equals(expectedLevel)) {
                String logMessage = logList.get(i).getLogMessage();
                if (logMessage.contains(expectedMessage)) count++;
            }
        }
        return count;
    }

    public static <T> void printList(List<T> list) {
        Log.info("PrintList:");
        Iterator<T> it = list.iterator();
        while (it.hasNext()) {
            Log.info(it.next().toString());
        }

    }
}