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

import org.apache.dubbo.metrics.model.TimePair;
import org.apache.dubbo.rpc.model.ApplicationModel;

/**
 * Mark certain types of events, allow automatic recording of start and end times, and provide time pairs
 */
public abstract class TimeCounterEvent extends MetricsEvent {

    private final TimePair timePair;

    public TimeCounterEvent(ApplicationModel source) {
        super(source);
        this.timePair = TimePair.start();
    }

    public TimePair getTimePair() {
        return timePair;
    }

}
