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
package org.apache.dubbo.metrics.collector.sample;

import org.apache.dubbo.common.logger.LogListener;
import org.apache.dubbo.common.logger.support.FailsafeErrorTypeAwareLogger;
import org.apache.dubbo.metrics.model.key.MetricsKey;

/**
 * Listen the log of all {@link FailsafeErrorTypeAwareLogger} instances, and add error code count to {@link ErrorCodeSampler}.
 */
public class ErrorCodeMetricsListenRegister implements LogListener {

    private final ErrorCodeSampler errorCodeSampler;

    public ErrorCodeMetricsListenRegister(ErrorCodeSampler errorCodeSampler) {
        FailsafeErrorTypeAwareLogger.registerGlobalListen(this);
        this.errorCodeSampler = errorCodeSampler;
        this.errorCodeSampler.addMetricName(MetricsKey.ERROR_CODE_COUNT.getName());
    }

    @Override
    public void onMessage(String code, String msg) {
        errorCodeSampler.inc(code, MetricsKey.ERROR_CODE_COUNT.getName());
    }
}
