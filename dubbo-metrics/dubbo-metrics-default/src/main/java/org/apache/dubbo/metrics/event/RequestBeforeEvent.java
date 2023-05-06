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

import org.apache.dubbo.metrics.MetricsConstants;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;

/**
 * Acts on MetricsClusterFilter to monitor exceptions that occur before request execution
 */
public class RequestBeforeEvent extends TimeCounterEvent {

    public RequestBeforeEvent(ApplicationModel source, TypeWrapper typeWrapper) {
        super(source, typeWrapper);

    }

    public static RequestBeforeEvent toEvent(ApplicationModel applicationModel, Invocation invocation) {
        RequestBeforeEvent event = new RequestBeforeEvent(applicationModel, new TypeWrapper(MetricsLevel.METHOD, MetricsKey.METRIC_REQUESTS));
        event.putAttachment(ATTACHMENT_KEY_SERVICE, MetricsSupport.getInterfaceName(invocation));
        event.putAttachment(MetricsConstants.INVOCATION_SIDE, MetricsSupport.getSide(invocation));
        event.putAttachment(MetricsConstants.INVOCATION, invocation);
        return event;
    }

}
