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

package org.apache.dubbo.spring.boot.actuate.mertics;

import org.apache.dubbo.metrics.DubboMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;


public class DubboMetricsBinder implements ApplicationListener<ApplicationStartedEvent>, DisposableBean {
    private final MeterRegistry meterRegistry;
    private volatile DubboMetrics dubboMetrics;

    public DubboMetricsBinder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }


    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {
         dubboMetrics = new DubboMetrics();
         dubboMetrics.bindTo(meterRegistry);
    }

    @Override
    public void destroy() throws Exception {
        dubboMetrics.destroy();
    }
}
