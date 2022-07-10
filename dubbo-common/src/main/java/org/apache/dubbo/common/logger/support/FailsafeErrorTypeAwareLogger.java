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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.utils.NetUtils;

/**
 * A fail-safe (ignoring exception thrown by logger) wrapper of error type aware logger.
 */
public class FailsafeErrorTypeAwareLogger extends FailsafeLogger implements ErrorTypeAwareLogger {

    /**
     * Mock address for formatting.
     */
    private static final String INSTRUCTIONS_URL = "https://dubbo.apache.org/faq/%s";

    public FailsafeErrorTypeAwareLogger(Logger logger) {
        super(logger);
    }

    private String appendContextMessageWithInstructions(String code, String cause, String extendedInformation, String msg) {
        return " [DUBBO] " + msg + ", dubbo version: " + Version.getVersion() +
            ", current host: " + NetUtils.getLocalHost() + ", error code: " + code +
            ". This may be caused by " + cause + ", " +
            "go to " + getErrorUrl(code) + " to find instructions. " + extendedInformation;
    }

    private String getErrorUrl(String code) {
        return String.format(INSTRUCTIONS_URL, code);
    }

    @Override
    public void trace(String code, String cause, String extendedInformation, String msg) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().trace(appendContextMessageWithInstructions(code, cause, extendedInformation, msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void trace(String code, String cause, String extendedInformation, String msg, Throwable e) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().trace(appendContextMessageWithInstructions(code, cause, extendedInformation, msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void debug(String code, String cause, String extendedInformation, String msg) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().debug(appendContextMessageWithInstructions(code, cause, extendedInformation, msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void debug(String code, String cause, String extendedInformation, String msg, Throwable e) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().debug(appendContextMessageWithInstructions(code, cause, extendedInformation, msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void info(String code, String cause, String extendedInformation, String msg) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().info(appendContextMessageWithInstructions(code, cause, extendedInformation, msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void info(String code, String cause, String extendedInformation, String msg, Throwable e) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().info(appendContextMessageWithInstructions(code, cause, extendedInformation, msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void warn(String code, String cause, String extendedInformation, String msg) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().warn(appendContextMessageWithInstructions(code, cause, extendedInformation, msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void warn(String code, String cause, String extendedInformation, String msg, Throwable e) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().warn(appendContextMessageWithInstructions(code, cause, extendedInformation, msg), e);
        } catch (Throwable t) {
        }
    }

    @Override
    public void error(String code, String cause, String extendedInformation, String msg) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().error(appendContextMessageWithInstructions(code, cause, extendedInformation, msg));
        } catch (Throwable t) {
        }
    }

    @Override
    public void error(String code, String cause, String extendedInformation, String msg, Throwable e) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().error(appendContextMessageWithInstructions(code, cause, extendedInformation, msg), e);
        } catch (Throwable t) {
        }
    }
}
