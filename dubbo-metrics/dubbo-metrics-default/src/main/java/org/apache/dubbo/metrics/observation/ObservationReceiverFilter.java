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
package org.apache.dubbo.metrics.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metrics.filter.observation.DefaultDubboServerObservationConvention;
import org.apache.dubbo.metrics.filter.observation.DubboObservation;
import org.apache.dubbo.metrics.filter.observation.DubboServerContext;
import org.apache.dubbo.metrics.filter.observation.DubboServerObservationConvention;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

/**
 * A {@link Filter} that creates an {@link Observation} around the incoming message.
 */
@Activate(group = PROVIDER, order = -1, onClass = "io.micrometer.observation.NoopObservationRegistry")
public class ObservationReceiverFilter implements Filter, BaseFilter.Listener, ScopeModelAware {

    private final ObservationRegistry observationRegistry;

    private final DubboServerObservationConvention serverObservationConvention;

    public ObservationReceiverFilter(ApplicationModel applicationModel) {
        observationRegistry = applicationModel.getBeanFactory().getBean(ObservationRegistry.class);
        serverObservationConvention = applicationModel.getBeanFactory().getBean(DubboServerObservationConvention.class);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (observationRegistry == null) {
            return invoker.invoke(invocation);
        }
        DubboServerContext receiverContext = new DubboServerContext(invoker, invocation);
        Observation observation = DubboObservation.SERVER.observation(this.serverObservationConvention, DefaultDubboServerObservationConvention.INSTANCE, () -> receiverContext, observationRegistry);
        invocation.put(Observation.class, observation.start());
        return observation.scoped(() -> invoker.invoke(invocation));
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        Observation observation = (Observation) invocation.get(Observation.class);
        if (observation == null) {
            return;
        }
        observation.stop();
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        Observation observation = (Observation) invocation.get(Observation.class);
        if (observation == null) {
            return;
        }
        observation.error(t);
        observation.stop();
    }
}
