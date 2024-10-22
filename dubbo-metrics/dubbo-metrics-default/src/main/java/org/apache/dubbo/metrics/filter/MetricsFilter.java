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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.event.MetricsDispatcher;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.service.CompatibleMetricsService;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.fastjson2.JSON;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.METRICS_SERVICE_PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METRICS_SERVICE_PROTOCOL_KEY;
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
    private static final AtomicBoolean exported = new AtomicBoolean(false);
    private Integer port;
    private String protocolName;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.rpcMetricsEnable = applicationModel
                .getApplicationConfigManager()
                .getMetrics()
                .map(MetricsConfig::getEnableRpc)
                .orElse(false);
        this.appName = applicationModel.tryGetApplicationName();
        this.metricsDispatcher = applicationModel.getBeanFactory().getBean(MetricsDispatcher.class);
        this.defaultMetricsCollector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
        serviceLevel = MethodMetric.isServiceLevel(applicationModel);
    }

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoke(invoker, invocation, PROVIDER.equals(MetricsSupport.getSide(invocation)));
    }

    public Result invoke(Invoker<?> invoker, Invocation invocation, boolean isProvider) throws RpcException {
        if (exported.compareAndSet(false, true)) {
            this.protocolName = invoker.getUrl().getParameter(METRICS_SERVICE_PROTOCOL_KEY) == null
                    ? DEFAULT_PROTOCOL
                    : invoker.getUrl().getParameter(METRICS_SERVICE_PROTOCOL_KEY);

            Protocol protocol =
                    ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolName);
            this.port = invoker.getUrl().getParameter(METRICS_SERVICE_PORT_KEY) == null
                    ? protocol.getDefaultPort()
                    : Integer.parseInt(invoker.getUrl().getParameter(METRICS_SERVICE_PORT_KEY));
            Invoker<CompatibleMetricsService> metricsInvoker = initMetricsInvoker(isProvider);
            try {
                protocol.export(metricsInvoker);
            } catch (RuntimeException e) {
                LOGGER.error("Metrics Service need to be configured" + " when multiple processes are running on a host"
                        + e.getMessage());
            }
        }
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
        if (rpcMetricsEnable) {
            onResponse(result, invoker, invocation, PROVIDER.equals(MetricsSupport.getSide(invocation)));
        }
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
        if (rpcMetricsEnable) {
            onError(t, invoker, invocation, PROVIDER.equals(MetricsSupport.getSide(invocation)));
        }
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

    private Invoker<CompatibleMetricsService> initMetricsInvoker(boolean isProvider) {
        return new Invoker<CompatibleMetricsService>() {
            @Override
            public Class<CompatibleMetricsService> getInterface() {
                return CompatibleMetricsService.class;
            }

            @Override
            public Result invoke(Invocation invocation) throws RpcException {

                List<MetricSample> collect = defaultMetricsCollector.collect();
                return AsyncRpcResult.newDefaultAsyncResult(JSON.toJSONString(collect), invocation);
            }

            @Override
            public URL getUrl() {
                return URL.valueOf(protocolName + "://" + NetUtils.getIpByConfig(applicationModel) + ":" + port + "/"
                        + CompatibleMetricsService.class.getName());
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public void destroy() {}
        };
    }
}
