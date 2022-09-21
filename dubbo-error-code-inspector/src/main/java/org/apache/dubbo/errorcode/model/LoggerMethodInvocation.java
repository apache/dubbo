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

package org.apache.dubbo.errorcode.model;

import java.util.List;

/**
 * Represents a logger method invocation in a class.
 */
public class LoggerMethodInvocation {
    private String loggerMethodInvocationCode;

    private List<String> occurredLines;

    public LoggerMethodInvocation() {
    }

    public LoggerMethodInvocation(String loggerMethodInvocationCode, List<String> occurredLines) {
        this.loggerMethodInvocationCode = loggerMethodInvocationCode;
        this.occurredLines = occurredLines;
    }

    @Override
    public String toString() {
        return "LoggerMethodInvocation{" +
            "loggerMethodInvocationCode='" + loggerMethodInvocationCode + '\'' +
            ", occurredLines=" + occurredLines +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoggerMethodInvocation that = (LoggerMethodInvocation) o;

        if (!loggerMethodInvocationCode.equals(that.loggerMethodInvocationCode)) return false;
        return occurredLines.equals(that.occurredLines);
    }

    @Override
    public int hashCode() {
        int result = loggerMethodInvocationCode.hashCode();
        result = 31 * result + occurredLines.hashCode();
        return result;
    }

    public String getLoggerMethodInvocationCode() {
        return loggerMethodInvocationCode;
    }

    public void setLoggerMethodInvocationCode(String loggerMethodInvocationCode) {
        this.loggerMethodInvocationCode = loggerMethodInvocationCode;
    }

    public List<String> getOccurredLines() {
        return occurredLines;
    }

    public void setOccurredLines(List<String> occurredLines) {
        this.occurredLines = occurredLines;
    }
}
