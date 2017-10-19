/*
 * Copyright 1999-2012 Alibaba Group.
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

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class LogHelper {

    private LogHelper() {
    }

    public static void trace(Logger logger, String msg) {
        if (logger == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(msg);
        }
    }

    public static void trace(Logger logger, Throwable throwable) {
        if (logger == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(throwable);
        }
    }

    public static void trace(Logger logger, String msg, Throwable e) {
        if (logger == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(msg, e);
        }
    }

    public static void debug(Logger logger, String msg) {
        if (logger == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(msg);
        }
    }

    public static void debug(Logger logger, Throwable e) {
        if (logger == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(e);
        }
    }

    public static void debug(Logger logger, String msg, Throwable e) {
        if (logger == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(msg, e);
        }
    }

    public static void info(Logger logger, String msg) {
        if (logger == null) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info(msg);
        }
    }

    public static void info(Logger logger, Throwable e) {
        if (logger == null) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info(e);
        }
    }

    public static void info(Logger logger, String msg, Throwable e) {
        if (logger == null) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info(msg, e);
        }
    }

    public static void warn(Logger logger, String msg, Throwable e) {
        if (logger == null) {
            return;
        }

        if (logger.isWarnEnabled()) {
            logger.warn(msg, e);
        }
    }

    public static void warn(Logger logger, String msg) {
        if (logger == null) {
            return;
        }

        if (logger.isWarnEnabled()) {
            logger.warn(msg);
        }
    }

    public static void warn(Logger logger, Throwable e) {
        if (logger == null) {
            return;
        }

        if (logger.isWarnEnabled()) {
            logger.warn(e);
        }
    }

    public static void error(Logger logger, Throwable e) {
        if (logger == null) {
            return;
        }

        if (logger.isErrorEnabled()) {
            logger.error(e);
        }
    }

    public static void error(Logger logger, String msg) {
        if (logger == null) {
            return;
        }

        if (logger.isErrorEnabled()) {
            logger.error(msg);
        }
    }

    public static void error(Logger logger, String msg, Throwable e) {
        if (logger == null) {
            return;
        }

        if (logger.isErrorEnabled()) {
            logger.error(msg, e);
        }
    }

}
