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
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUEST_BUSINESS_FAILED;

public class RequestInitEvent extends RequestEvent {


    public RequestInitEvent(ApplicationModel applicationModel, String appName, MetricsDispatcher metricsDispatcher, DefaultMetricsCollector collector, TypeWrapper TYPE_WRAPPER) {
        super(applicationModel, appName, metricsDispatcher, collector, TYPE_WRAPPER);
    }

    public static RequestInitEvent toRequestInitEvent(ApplicationModel applicationModel, Invocation invocation) {

        RequestInitEvent requestEvent = new RequestInitEvent(applicationModel, null,null,null,new TypeWrapper(MetricsLevel.SERVICE, METRIC_REQUESTS, METRIC_REQUESTS_SUCCEED, METRIC_REQUEST_BUSINESS_FAILED));
        requestEvent.putAttachment(MetricsConstants.INVOCATION, invocation);
        MethodMetric methodMetric = new MethodMetric(applicationModel, invocation);
        requestEvent.putAttachment(MetricsConstants.METHOD_METRICS, methodMetric);
        requestEvent.putAttachment(ATTACHMENT_KEY_SERVICE, MetricsSupport.getInterfaceName(invocation));
        requestEvent.putAttachment(MetricsConstants.INVOCATION_SIDE, MetricsSupport.getSide(invocation));
        return requestEvent;
    }


}
