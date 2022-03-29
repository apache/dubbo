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
package org.apache.dubbo.common.logger.support;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.utils.NetUtils;

public class FailsafeLogger implements Logger {

    private Logger logger;

    private static boolean disabled = false;

    public FailsafeLogger(Logger logger) {
        this.logger = logger;
    }

    public static void setDisabled(boolean disabled) {
        FailsafeLogger.disabled = disabled;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    private String appendContextMessage(String msg) {
        return " [DUBBO] " + msg + ", dubbo version: " + Version.getVersion() + ", current host: " + NetUtils.getLocalHost();
    }

    @Override
    public void trace(String msg, Throwable e) {
        if (disabled) {
            return;
        }
        try {
            logger.trace(appendContextMessage(msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void trace(Throwable e) {
        if (disabled) {
            return;
        }
        try {
            logger.trace(e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void trace(String msg) {
        if (disabled) {
            return;
        }
        try {
            logger.trace(appendContextMessage(msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void debug(String msg, Throwable e) {
        if (disabled) {
            return;
        }
        try {
            logger.debug(appendContextMessage(msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void debug(Throwable e) {
        if (disabled) {
            return;
        }
        try {
            logger.debug(e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void debug(String msg) {
        if (disabled) {
            return;
        }
        try {
            logger.debug(appendContextMessage(msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void info(String msg, Throwable e) {
        if (disabled) {
            return;
        }
        try {
            logger.info(appendContextMessage(msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void info(String msg) {
        if (disabled) {
            return;
        }
        try {
            logger.info(appendContextMessage(msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void warn(String msg, Throwable e) {
        if (disabled) {
            return;
        }
        try {
            logger.warn(appendContextMessage(msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void warn(String msg) {
        if (disabled) {
            return;
        }
        try {
            logger.warn(appendContextMessage(msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void error(String msg, Throwable e) {
        if (disabled) {
            return;
        }
        try {
            logger.error(appendContextMessage(msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void error(String msg) {
        if (disabled) {
            return;
        }
        try {
            logger.error(appendContextMessage(msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void error(Throwable e) {
        if (disabled) {
            return;
        }
        try {
            logger.error(e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void info(Throwable e) {
        if (disabled) {
            return;
        }
        try {
            logger.info(e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void warn(Throwable e) {
        if (disabled) {
            return;
        }
        try {
            logger.warn(e);
        } catch (Throwable t) {
        }
    }

    @Override
    public boolean isTraceEnabled() {
        if (disabled) {
            return false;
        }
        try {
            return logger.isTraceEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isDebugEnabled() {
        if (disabled) {
            return false;
        }
        try {
            return logger.isDebugEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isInfoEnabled() {
        if (disabled) {
            return false;
        }
        try {
            return logger.isInfoEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isWarnEnabled() {
        if (disabled) {
            return false;
        }
        try {
            return logger.isWarnEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isErrorEnabled() {
        if (disabled) {
            return false;
        }
        try {
            return logger.isErrorEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

}
