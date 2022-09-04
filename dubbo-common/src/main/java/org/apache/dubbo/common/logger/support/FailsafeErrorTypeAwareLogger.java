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

import java.util.regex.Pattern;

/**
 * A fail-safe (ignoring exception thrown by logger) wrapper of error type aware logger.
 */
public class FailsafeErrorTypeAwareLogger extends FailsafeLogger implements ErrorTypeAwareLogger {

    /**
     * Template address for formatting.
     */
    private static final String INSTRUCTIONS_URL = "https://dubbo.apache.org/faq/%d/%d";

    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\d+-\\d+");

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

        String trimmedString = code.trim();

        if (!ERROR_CODE_PATTERN.matcher(trimmedString).matches()) {
            error("Invalid error code: " + code + ", the format of error code is: X-X (where X is a number).");
            return "";
        }

        String[] segments = trimmedString.split("[-]");

        int[] errorCodeSegments = new int[2];

        try {
            errorCodeSegments[0] = Integer.parseInt(segments[0]);
            errorCodeSegments[1] = Integer.parseInt(segments[1]);
        } catch (NumberFormatException numberFormatException) {
            error("Invalid error code: " + code + ", the format of error code is: X-X (where X is a number).",
                numberFormatException);

            return "";
        }

        return String.format(INSTRUCTIONS_URL, errorCodeSegments[0], errorCodeSegments[1]);
    }

    @Override
    public void warn(String code, String cause, String extendedInformation, String msg) {
        if (getDisabled()) {
            return;
        }

        try {
            getLogger().warn(appendContextMessageWithInstructions(code, cause, extendedInformation, msg));
        } catch (Throwable t) {
            // ignored.
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
            // ignored.
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
            // ignored.
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
            // ignored.
        }
    }
}
