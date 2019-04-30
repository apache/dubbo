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

import org.apache.log4j.Level;

import java.io.Serializable;

public class Log implements Serializable {
    private static final long serialVersionUID = -534113138054377073L;
    private String logName;
    private Level logLevel;
    private String logMessage;
    private String logThread;

    public String getLogName() {
        return logName;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public void setLogMessage(String logMessage) {
        this.logMessage = logMessage;
    }

    public String getLogThread() {
        return logThread;
    }

    public void setLogThread(String logThread) {
        this.logThread = logThread;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((logLevel == null) ? 0 : logLevel.hashCode());
        result = prime * result + ((logMessage == null) ? 0 : logMessage.hashCode());
        result = prime * result + ((logName == null) ? 0 : logName.hashCode());
        result = prime * result + ((logThread == null) ? 0 : logThread.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Log other = (Log) obj;
        if (logLevel == null) {
            if (other.logLevel != null) {
                return false;
            }
        } else if (!logLevel.equals(other.logLevel)) {
            return false;
        }
        if (logMessage == null) {
            if (other.logMessage != null) {
                return false;
            }
        } else if (!logMessage.equals(other.logMessage)) {
            return false;
        }
        if (logName == null) {
            if (other.logName != null) {
                return false;
            }
        } else if (!logName.equals(other.logName)) {
            return false;
        }
        if (logThread == null) {
            if (other.logThread != null) {
                return false;
            }
        } else if (!logThread.equals(other.logThread)) {
            return false;
        }
        return true;
    }

}