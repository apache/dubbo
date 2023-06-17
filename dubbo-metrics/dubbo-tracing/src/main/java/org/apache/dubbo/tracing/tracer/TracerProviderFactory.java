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
package org.apache.dubbo.tracing.tracer;

import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.tracing.tracer.brave.BraveProvider;
import org.apache.dubbo.tracing.tracer.otel.OpenTelemetryProvider;
import org.apache.dubbo.tracing.utils.ObservationSupportUtil;

public class TracerProviderFactory {

    public static TracerProvider getProvider(ApplicationModel applicationModel, TracingConfig tracingConfig) {
        // If support OTel firstly, return OTel, then Brave.
        if (ObservationSupportUtil.isSupportOTelTracer()) {
            return new OpenTelemetryProvider(applicationModel, tracingConfig);
        }

        if (ObservationSupportUtil.isSupportBraveTracer()) {
            return new BraveProvider(applicationModel, tracingConfig);
        }

        return null;
    }
}
