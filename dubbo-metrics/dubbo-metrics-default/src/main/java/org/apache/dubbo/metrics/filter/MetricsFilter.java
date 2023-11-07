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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.event.MetricsDispatcher;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.metrics.DefaultConstants.METRIC_FILTER_EVENT;
import static org.apache.dubbo.metrics.DefaultConstants.METRIC_THROWABLE;

public class MetricsFilter implements ScopeModelAware {

    private ApplicationModel applicationModel;
    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(MetricsFilter.class);
    private boolean rpcMetricsEnable;
    private String appName;
    private MetricsDispatcher metricsDispatcher;
    private DefaultMetricsCollector defaultMetricsCollector;
    private boolean serviceLevel;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.rpcMetricsEnable = applicationModel
                .getApplicationConfigManager()
                .getMetrics()
                .map(MetricsConfig::getEnableRpc)
                .orElse(true);
        this.appName = applicationModel.tryGetApplicationName();
        this.metricsDispatcher = applicationModel.getBeanFactory().getBean(MetricsDispatcher.class);
        this.defaultMetricsCollector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
        serviceLevel = MethodMetric.isServiceLevel(applicationModel);
    }

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoke(invoker, invocation, PROVIDER.equals(MetricsSupport.getSide(invocation)));
    }

    public Result invoke(Invoker<?> invoker, Invocation invocation, boolean isProvider) throws RpcException {
        if (rpcMetricsEnable) {
            try {
                RequestEvent requestEvent = RequestEvent.toRequestEvent(
                        applicationModel,
                        appName,
                        metricsDispatcher,
                        defaultMetricsCollector,
                        invocation,
                        isProvider ? PROVIDER : CONSUMER,
                        serviceLevel);
                MetricsEventBus.before(requestEvent);
                invocation.put(METRIC_FILTER_EVENT, requestEvent);
            } catch (Throwable t) {
                LOGGER.warn(INTERNAL_ERROR, "", "", "Error occurred when invoke.", t);
            }
        }
        return invoker.invoke(invocation);
    }

    public void onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        onResponse(result, invoker, invocation, PROVIDER.equals(MetricsSupport.getSide(invocation)));
    }

    public void onResponse(Result result, Invoker<?> invoker, Invocation invocation, boolean isProvider) {
        Object eventObj = invocation.get(METRIC_FILTER_EVENT);
        if (eventObj != null) {
            try {
                MetricsEventBus.after((RequestEvent) eventObj, result);
            } catch (Throwable t) {
                LOGGER.warn(INTERNAL_ERROR, "", "", "Error occurred when onResponse.", t);
            }
        }
    }

    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        onError(t, invoker, invocation, PROVIDER.equals(MetricsSupport.getSide(invocation)));
    }

    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation, boolean isProvider) {
        Object eventObj = invocation.get(METRIC_FILTER_EVENT);
        if (eventObj != null) {
            try {
                RequestEvent requestEvent = (RequestEvent) eventObj;
                requestEvent.putAttachment(METRIC_THROWABLE, t);
                MetricsEventBus.error(requestEvent);
            } catch (Throwable throwable) {
                LOGGER.warn(INTERNAL_ERROR, "", "", "Error occurred when onResponse.", throwable);
            }
        }
    }
}
