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
import org.apache.dubbo.common.logger.ErrorType;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.utils.NetUtils;

/**
 * A fail-safe (ignoring exception thrown by logger) wrapper of error type aware logger.
 */
public class FailsafeErrorTypeAwareLogger extends FailsafeLogger implements ErrorTypeAwareLogger {

    public FailsafeErrorTypeAwareLogger(Logger logger) {
        super(logger);
    }

    private String appendContextMessageWithInstructions(ErrorType errorType, String msg) {
        return " [DUBBO] " + msg + ", dubbo version: " + Version.getVersion() +
            ", current host: " + NetUtils.getLocalHost() + ", error code: " +
            ". This may be caused by " + errorType.getCause() + ", " +
            "go to " + errorType.getErrorUrl() + " to find instructions.";
    }

    @Override
    public void trace(ErrorType errorType, String msg) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().trace(appendContextMessageWithInstructions(errorType, msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void trace(ErrorType errorType, String msg, Throwable e) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().trace(appendContextMessageWithInstructions(errorType, msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void debug(ErrorType errorType, String msg) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().debug(appendContextMessageWithInstructions(errorType, msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void debug(ErrorType errorType, String msg, Throwable e) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().debug(appendContextMessageWithInstructions(errorType, msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void info(ErrorType errorType, String msg) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().info(appendContextMessageWithInstructions(errorType, msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void info(ErrorType errorType, String msg, Throwable e) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().info(appendContextMessageWithInstructions(errorType, msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void warn(ErrorType errorType, String msg) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().warn(appendContextMessageWithInstructions(errorType, msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void warn(ErrorType errorType, String msg, Throwable e) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().warn(appendContextMessageWithInstructions(errorType, msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void error(ErrorType errorType, String msg) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().error(appendContextMessageWithInstructions(errorType, msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void error(ErrorType errorType, String msg, Throwable e) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().error(appendContextMessageWithInstructions(errorType, msg), e);
        } catch (Throwable t) {
        }
    }
}
