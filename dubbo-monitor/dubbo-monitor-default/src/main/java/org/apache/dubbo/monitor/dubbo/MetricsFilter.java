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
package org.apache.dubbo.monitor.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionAccessor;
import org.apache.dubbo.common.extension.ExtensionAccessorAware;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.store.DataStore;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.monitor.MetricsService;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.rpc.support.RpcUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.metrics.FastCompass;
import com.alibaba.metrics.MetricLevel;
import com.alibaba.metrics.MetricManager;
import com.alibaba.metrics.MetricName;
import com.alibaba.metrics.MetricRegistry;
import com.alibaba.metrics.common.CollectLevel;
import com.alibaba.metrics.common.MetricObject;
import com.alibaba.metrics.common.MetricsCollector;
import com.alibaba.metrics.common.MetricsCollectorFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.monitor.Constants.DUBBO_CONSUMER;
import static org.apache.dubbo.monitor.Constants.DUBBO_CONSUMER_METHOD;
import static org.apache.dubbo.monitor.Constants.DUBBO_GROUP;
import static org.apache.dubbo.monitor.Constants.DUBBO_PROVIDER;
import static org.apache.dubbo.monitor.Constants.DUBBO_PROVIDER_METHOD;
import static org.apache.dubbo.monitor.Constants.SERVICE;

/**
 * @deprecated After metrics config is refactored.
 * This filter should no longer use and will be deleted in the future.
 */
@Deprecated
public class MetricsFilter implements Filter, ExtensionAccessorAware, ScopeModelAware {

    private static final Logger logger = LoggerFactory.getLogger(MetricsFilter.class);
    protected static volatile AtomicBoolean exported = new AtomicBoolean(false);
    private Integer port;
    private String protocolName;
    private ExtensionAccessor extensionAccessor;
    private ApplicationModel applicationModel;

    private static final String METRICS_PORT = "metrics.port";
    private static final String METRICS_PROTOCOL = "metrics.protocol";

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (exported.compareAndSet(false, true)) {
            this.protocolName = invoker.getUrl().getParameter(METRICS_PROTOCOL) == null ?
                    DEFAULT_PROTOCOL : invoker.getUrl().getParameter(METRICS_PROTOCOL);

            Protocol protocol = extensionAccessor.getExtensionLoader(Protocol.class).getExtension(protocolName);

            this.port = invoker.getUrl().getParameter(METRICS_PORT) == null ?
                    protocol.getDefaultPort() : Integer.valueOf(invoker.getUrl().getParameter(METRICS_PORT));

            Invoker<MetricsService> metricsInvoker = initMetricsInvoker();

            try {
                protocol.export(metricsInvoker);
            } catch (RuntimeException e) {
                logger.error("Metrics Service need to be configured" +
                        " when multiple processes are running on a host" + e.getMessage());
            }
        }

        boolean isProvider = invoker.getUrl().getSide(PROVIDER).equalsIgnoreCase(PROVIDER);
        long start = System.currentTimeMillis();
        try {
            Result result = invoker.invoke(invocation); // proceed invocation chain
            long duration = System.currentTimeMillis() - start;
            reportMetrics(invoker, invocation, duration, "success", isProvider);
            return result;
        } catch (RpcException e) {
            long duration = System.currentTimeMillis() - start;
            String result = "error";
            if (e.isTimeout()) {
                result = "timeoutError";
            }
            if (e.isBiz()) {
                result = "bisError";
            }
            if (e.isNetwork()) {
                result = "networkError";
            }
            if (e.isSerialization()) {
                result = "serializationError";
            }
            reportMetrics(invoker, invocation, duration, result, isProvider);
            throw e;
        }
    }

    private String buildMethodName(Invocation invocation) {
        String methodName = RpcUtils.getMethodName(invocation);
        StringBuilder method = new StringBuilder(methodName);
        Class<?>[] argTypes = RpcUtils.getParameterTypes(invocation);

        method.append('(');

        for (int i = 0; i < argTypes.length; i++) {
            method.append((i == 0 ? "" : ", ") + argTypes[i].getSimpleName());
        }
        method.append(')');
        Class<?> returnType = RpcUtils.getReturnType(invocation);
        String typeName = null;
        if(returnType != null) {
            typeName = returnType.getTypeName();
            typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
        }

        return (typeName == null ? "void" : typeName) + " " + method;
    }

    private void reportMetrics(Invoker<?> invoker, Invocation invocation, long duration, String result, boolean isProvider) {
        String serviceName = invoker.getInterface().getName();
        String methodName = buildMethodName(invocation);
        MetricName global;
        MetricName method;
        if (isProvider) {
            global = new MetricName(DUBBO_PROVIDER, MetricLevel.MAJOR);
            method = new MetricName(DUBBO_PROVIDER_METHOD, new HashMap<String, String>(4) {
                {
                    put(SERVICE, serviceName);
                    put(METHOD_KEY, methodName);
                }
            }, MetricLevel.NORMAL);
        } else {
            global = new MetricName(DUBBO_CONSUMER, MetricLevel.MAJOR);
            method = new MetricName(DUBBO_CONSUMER_METHOD, new HashMap<String, String>(4) {
                {
                    put(SERVICE, serviceName);
                    put(METHOD_KEY, methodName);
                }
            }, MetricLevel.NORMAL);
        }
        setCompassQuantity(DUBBO_GROUP, result, duration, global, method);
    }

    private void setCompassQuantity(String groupName, String result, long duration, MetricName... metricNames) {
        for (MetricName metricName : metricNames) {
            FastCompass compass = MetricManager.getFastCompass(groupName, metricName);
            compass.record(duration, result);
        }
    }

    private List<MetricObject> getThreadPoolMessage() {
        DataStore dataStore = extensionAccessor.getExtensionLoader(DataStore.class).getDefaultExtension();
        Map<String, Object> executors = dataStore.get(EXECUTOR_SERVICE_COMPONENT_KEY);

        List<MetricObject> threadPoolMtricList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : executors.entrySet()) {
            String port = entry.getKey();
            ExecutorService executor = (ExecutorService) entry.getValue();
            if (executor instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor tp = (ThreadPoolExecutor) executor;

                threadPoolMtricList.add(value2MetricObject("threadPool.active", tp.getActiveCount(), MetricLevel.MAJOR));
                threadPoolMtricList.add(value2MetricObject("threadPool.core", tp.getCorePoolSize(), MetricLevel.MAJOR));
                threadPoolMtricList.add(value2MetricObject("threadPool.max", tp.getMaximumPoolSize(), MetricLevel.MAJOR));
                threadPoolMtricList.add(value2MetricObject("threadPool.current", tp.getPoolSize(), MetricLevel.MAJOR));
            }
        }

        return threadPoolMtricList;
    }

    private MetricObject value2MetricObject(String metric, Integer value, MetricLevel level) {
        if (metric == null || value == null || level == null) {
            return null;
        }

        return new MetricObject
                .Builder(metric)
                .withValue(value)
                .withLevel(level)
                .build();
    }

    private Invoker<MetricsService> initMetricsInvoker() {
        Invoker<MetricsService> metricsInvoker = new Invoker<MetricsService>() {
            @Override
            public Class<MetricsService> getInterface() {
                return MetricsService.class;
            }

            @Override
            public Result invoke(Invocation invocation) throws RpcException {
                String group = invocation.getArguments()[0].toString();
                MetricRegistry registry = MetricManager.getIMetricManager().getMetricRegistryByGroup(group);

                SortedMap<MetricName, FastCompass> fastCompasses = registry.getFastCompasses();

                long timestamp = System.currentTimeMillis();
                double rateFactor = TimeUnit.SECONDS.toSeconds(1);
                double durationFactor = 1.0 / TimeUnit.MILLISECONDS.toNanos(1);


                MetricsCollector collector = MetricsCollectorFactory.createNew(
                        CollectLevel.NORMAL, Collections.EMPTY_MAP, rateFactor, durationFactor, null);

                for (Map.Entry<MetricName, FastCompass> entry : fastCompasses.entrySet()) {
                    collector.collect(entry.getKey(), entry.getValue(), timestamp);
                }

                List res = collector.build();
                res.addAll(getThreadPoolMessage());
                return AsyncRpcResult.newDefaultAsyncResult(JSON.toJSONString(res), invocation);
            }

            @Override
            public URL getUrl() {
                return URL.valueOf(protocolName + "://" + NetUtils.getIpByConfig(applicationModel) + ":" + port + "/" + MetricsService.class.getName());
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public void destroy() {

            }
        };

        return metricsInvoker;
    }

    @Override
    public void setExtensionAccessor(ExtensionAccessor extensionAccessor) {
        this.extensionAccessor = extensionAccessor;
    }
}
