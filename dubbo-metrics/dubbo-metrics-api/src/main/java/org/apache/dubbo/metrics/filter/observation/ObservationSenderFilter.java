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
package org.apache.dubbo.metrics.filter.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

/**
 * A {@link Filter} that creates an {@link Observation} around the outgoing message.
 */
@Activate(group = CONSUMER, order = -1)
public class ObservationSenderFilter implements ClusterFilter, BaseFilter.Listener, ScopeModelAware {

    private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

    private DubboProviderObservationConvention providerObservationConvention = null;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        observationRegistry = applicationModel.getBeanFactory().getBean(ObservationRegistry.class);
        providerObservationConvention = applicationModel.getBeanFactory().getBean(DubboProviderObservationConvention.class);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (observationRegistry == null) {
            return invoker.invoke(invocation);
        }
        RpcContextAttachment context = RpcContext.getClientAttachment();
        DubboClientContext senderContext = new DubboClientContext(invocation.getAttachments(), context, invoker, invocation);
        Observation observation = DubboObservation.CLIENT.observation(this.providerObservationConvention, DefaultDubboClientObservationConvention.INSTANCE, () -> senderContext, observationRegistry);
        return observation.observe(() -> invoker.invoke(invocation));
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {

    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {

    }
}
