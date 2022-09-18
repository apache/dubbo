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

package org.apache.dubbo.errorcode.util;

import org.apache.dubbo.errorcode.extractor.ErrorCodeExtractor;

/**
 * Utilities of generating URLs.
 */
public final class ErrorUrlUtils {
    private ErrorUrlUtils() {
        throw new UnsupportedOperationException("No instance of ErrorUrlUtils for you! ");
    }

    private static final String INSTRUCTIONS_URL = "https://dubbo.apache.org/faq/%d/%d";

    public static String getErrorUrl(String code) {

        String trimmedString = code.trim();

        if (!ErrorCodeExtractor.ERROR_CODE_PATTERN.matcher(trimmedString).matches()) {
            return "";
        }

        String[] segments = trimmedString.split("-");

        int[] errorCodeSegments = new int[2];

        try {
            errorCodeSegments[0] = Integer.parseInt(segments[0]);
            errorCodeSegments[1] = Integer.parseInt(segments[1]);
        } catch (NumberFormatException numberFormatException) {
            return "";
        }

        return String.format(INSTRUCTIONS_URL, errorCodeSegments[0], errorCodeSegments[1]);
    }

    public static String getErrorCodeThroughErrorUrl(String link) {
        return link.replace("https://dubbo.apache.org/faq/", "")
            .replace("/", "-");
    }
}
