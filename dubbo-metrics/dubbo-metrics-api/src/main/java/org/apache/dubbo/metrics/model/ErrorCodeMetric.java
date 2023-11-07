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
package org.apache.dubbo.metrics.model;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_ERROR_CODE;

/**
 * ErrorCodeMetric. Provide tags for error code metrics.
 */
public class ErrorCodeMetric implements Metric {

    private final String errorCode;

    private final String applicationName;

    public ErrorCodeMetric(String applicationName, String errorCode) {
        this.errorCode = errorCode;
        this.applicationName = applicationName;
    }

    @Override
    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_ERROR_CODE, errorCode);
        tags.put(TAG_APPLICATION_NAME, applicationName);
        return tags;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
