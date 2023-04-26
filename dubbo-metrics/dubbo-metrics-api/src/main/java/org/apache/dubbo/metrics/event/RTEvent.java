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

package org.apache.dubbo.metrics.event;

import org.apache.dubbo.rpc.model.ApplicationModel;

/**
 * RtEvent.
 */
public class RTEvent extends MetricsEvent {
    private Long rt;
    private final Object metric;

    public RTEvent(ApplicationModel applicationModel, Object metric, Long rt) {
        super(applicationModel);
        this.rt = rt;
        this.metric = metric;
        setAvailable(true);
    }

    public Long getRt() {
        return rt;
    }

    public void setRt(Long rt) {
        this.rt = rt;
    }

    public Object getMetric() {
        return metric;
    }
}
