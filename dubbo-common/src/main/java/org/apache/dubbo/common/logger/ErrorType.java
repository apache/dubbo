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

package org.apache.dubbo.common.logger;

/**
 * Represents an error type, which can be displayed by the logger like:
 * ".... This may be caused by [error cause], go to [URL] to find instructions."
 */
public enum ErrorType {

    // Temporarily empty. The only one entry is for testing.
    REGISTRY_CENTER_OFFLINE("1-1", "registry center", "http://dubbo.apache.org/");

    /**
     * Error code.
     */
    private final String code;

    /**
     * Error cause.
     */
    private final String cause;

    /**
     * Link to instructions.
     */
    private final String errorUrl;

    ErrorType(String code, String cause, String errorUrl) {
        this.code = code;
        this.cause = cause;
        this.errorUrl = errorUrl;
    }

    public String getCause() {
        return cause;
    }

    public String getErrorUrl() {
        return errorUrl;
    }

    public String getCode() {
        return code;
    }
}
