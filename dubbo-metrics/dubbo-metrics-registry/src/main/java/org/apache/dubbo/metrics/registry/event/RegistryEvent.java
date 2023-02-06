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

package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.TimeAble;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.TimePair;

public class RegistryEvent<S> extends MetricsEvent<S> implements TimeAble {
    private Type type;
    private TimePair timePair;

    public RegistryEvent(S source, Type type) {
        super(source);
        this.type = type;
    }

    public RegistryEvent(S source, TimePair timePair) {
        super(source);
        this.timePair = timePair;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public TimePair getTimePair() {
        return timePair;
    }

    public enum Type {
        TOTAL(MetricsKey.REGISTER_METRIC_REQUESTS),
        SUCCEED(MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED),
        FAILED(MetricsKey.REGISTER_METRIC_REQUESTS_FAILED);

        private final MetricsKey metricsKey;


        Type(MetricsKey metricsKey) {
            this.metricsKey = metricsKey;
        }

        public MetricsKey getMetricsKey() {
            return metricsKey;
        }
    }
}
