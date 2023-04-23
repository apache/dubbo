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
package org.apache.dubbo.metrics.filter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.metrics.DefaultConstants.METRIC_FILTER_EVENT;
import static org.apache.dubbo.metrics.DefaultConstants.METRIC_THROWABLE;

@Activate(group = {CONSUMER, PROVIDER}, order = -1)
public class MetricsFilter implements Filter, BaseFilter.Listener, ScopeModelAware {

    private ApplicationModel applicationModel;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RequestEvent requestEvent = RequestEvent.toRequestEvent(applicationModel, invocation);
        MetricsEventBus.before(requestEvent, () -> invocation.put(METRIC_FILTER_EVENT, requestEvent));
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        Object eventObj = invocation.get(METRIC_FILTER_EVENT);
        if (eventObj != null) {
            MetricsEventBus.after((RequestEvent) eventObj, result);
        }
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        Object eventObj = invocation.get(METRIC_FILTER_EVENT);
        if (eventObj != null) {
            RequestEvent requestEvent = (RequestEvent) eventObj;
            requestEvent.putAttachment(METRIC_THROWABLE, t);
            MetricsEventBus.error(requestEvent);
        }
    }


}
