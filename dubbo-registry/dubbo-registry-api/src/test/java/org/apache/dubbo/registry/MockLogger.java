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
package org.apache.dubbo.registry;

import org.apache.dubbo.common.logger.Logger;

import java.util.HashSet;
import java.util.Set;

public class MockLogger implements Logger {
    public Set<String> printedLogs = new HashSet<>();

    public boolean checkLogHappened(String msgPrefix) {
        for (String printedLog : printedLogs) {
            if (printedLog.contains(msgPrefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void trace(String msg) {

    }

    @Override
    public void trace(Throwable e) {

    }

    @Override
    public void trace(String msg, Throwable e) {

    }

    @Override
    public void debug(String msg) {

    }

    @Override
    public void debug(Throwable e) {

    }

    @Override
    public void debug(String msg, Throwable e) {

    }

    @Override
    public void info(String msg) {

    }

    @Override
    public void info(Throwable e) {

    }

    @Override
    public void info(String msg, Throwable e) {

    }

    @Override
    public void warn(String msg) {

    }

    @Override
    public void warn(Throwable e) {

    }

    @Override
    public void warn(String msg, Throwable e) {
        printedLogs.add(msg);
    }

    @Override
    public void error(String msg) {

    }

    @Override
    public void error(Throwable e) {

    }

    @Override
    public void error(String msg, Throwable e) {

    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    @Override
    public boolean isErrorEnabled() {
        return false;
    }
}
