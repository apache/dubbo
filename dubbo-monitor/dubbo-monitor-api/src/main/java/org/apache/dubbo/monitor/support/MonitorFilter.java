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
package org.apache.dubbo.monitor.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.monitor.Monitor;
import org.apache.dubbo.monitor.MonitorFactory;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.monitor.Constants.CONCURRENT_KEY;
import static org.apache.dubbo.monitor.Constants.COUNT_PROTOCOL;
import static org.apache.dubbo.monitor.Constants.ELAPSED_KEY;
import static org.apache.dubbo.monitor.Constants.FAILURE_KEY;
import static org.apache.dubbo.monitor.Constants.SUCCESS_KEY;
import static org.apache.dubbo.rpc.Constants.INPUT_KEY;
import static org.apache.dubbo.rpc.Constants.OUTPUT_KEY;

/**
 * MonitorFilter. (SPI, Singleton, ThreadSafe)
 */
@Activate(group = {PROVIDER})
public class MonitorFilter implements Filter, Filter.Listener {

    private static final Logger logger = LoggerFactory.getLogger(MonitorFilter.class);
    private static final String MONITOR_FILTER_START_TIME = "monitor_filter_start_time";
    private static final String MONITOR_REMOTE_HOST_STORE = "monitor_remote_host_store";

    /**
     * The Concurrent counter
     */
    private final ConcurrentMap<String, AtomicInteger> concurrents = new ConcurrentHashMap<>();

    /**
     * The MonitorFactory
     */
    private MonitorFactory monitorFactory;

    public void setMonitorFactory(MonitorFactory monitorFactory) {
        this.monitorFactory = monitorFactory;
    }


    /**
     * The invocation interceptor,it will collect the invoke data about this invocation and send it to monitor center
     *
     * @param invoker    service
     * @param invocation invocation.
     * @return {@link Result} the invoke result
     * @throws RpcException
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (invoker.getUrl().hasAttribute(MONITOR_KEY)) {
            invocation.put(MONITOR_FILTER_START_TIME, System.currentTimeMillis());
            invocation.put(MONITOR_REMOTE_HOST_STORE, RpcContext.getServiceContext().getRemoteHost());
            // count up
            getConcurrent(invoker, invocation).incrementAndGet();
        }
        // proceed invocation chain
        return invoker.invoke(invocation);
    }

    /**
     * concurrent counter
     *
     * @param invoker
     * @param invocation
     * @return
     */
    private AtomicInteger getConcurrent(Invoker<?> invoker, Invocation invocation) {
        String key = invoker.getInterface().getName() + "." + invocation.getMethodName();
        return concurrents.computeIfAbsent(key, k -> new AtomicInteger());
    }

    @Override
    public void onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        if (invoker.getUrl().hasAttribute(MONITOR_KEY)) {
            Long startTime = (Long) invocation.get(MONITOR_FILTER_START_TIME);
            if (startTime != null) {
                collect(invoker, invocation, result, (String) invocation.get(MONITOR_REMOTE_HOST_STORE), startTime, false);
            }
            // count down
            getConcurrent(invoker, invocation).decrementAndGet();
        }
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        if (invoker.getUrl().hasAttribute(MONITOR_KEY)) {
            Long startTime = (Long) invocation.get(MONITOR_FILTER_START_TIME);
            if (startTime != null) {
                collect(invoker, invocation, null, (String) invocation.get(MONITOR_REMOTE_HOST_STORE), startTime, true);
            }
            // count down
            getConcurrent(invoker, invocation).decrementAndGet();
        }
    }

    /**
     * The collector logic, it will be handled by the default monitor
     *
     * @param invoker
     * @param invocation
     * @param result     the invocation result
     * @param remoteHost the remote host address
     * @param start      the timestamp the invocation begin
     * @param error      if there is an error on the invocation
     */
    private void collect(Invoker<?> invoker, Invocation invocation, Result result, String remoteHost, long start, boolean error) {
        try {
            Object monitorUrl;
            monitorUrl = invoker.getUrl().getAttribute(MONITOR_KEY);
            if (monitorUrl instanceof URL) {
                Monitor monitor = monitorFactory.getMonitor((URL) monitorUrl);
                if (monitor == null) {
                    return;
                }
                URL statisticsUrl = createStatisticsUrl(invoker, invocation, result, remoteHost, start, error);
                monitor.collect(statisticsUrl.toSerializableURL());
            }
        } catch (Throwable t) {
            logger.warn("Failed to monitor count service " + invoker.getUrl() + ", cause: " + t.getMessage(), t);
        }
    }

    /**
     * Create statistics url
     *
     * @param invoker
     * @param invocation
     * @param result
     * @param remoteHost
     * @param start
     * @param error
     * @return
     */
    private URL createStatisticsUrl(Invoker<?> invoker, Invocation invocation, Result result, String remoteHost, long start, boolean error) {
        // ---- service statistics ----
        // invocation cost
        long elapsed = System.currentTimeMillis() - start;
        // current concurrent count
        int concurrent = getConcurrent(invoker, invocation).get();
        String application = invoker.getUrl().getApplication();
        // service name
        String service = invoker.getInterface().getName();
        // method name
        String method = RpcUtils.getMethodName(invocation);
        String group = invoker.getUrl().getGroup();
        String version = invoker.getUrl().getVersion();

        int localPort;
        String remoteKey, remoteValue;
        if (CONSUMER_SIDE.equals(invoker.getUrl().getSide())) {
            // ---- for service consumer ----
            localPort = 0;
            remoteKey = PROVIDER;
            remoteValue = invoker.getUrl().getAddress();
        } else {
            // ---- for service provider ----
            localPort = invoker.getUrl().getPort();
            remoteKey = CONSUMER;
            remoteValue = remoteHost;
        }
        String input = "", output = "";
        if (invocation.getAttachment(INPUT_KEY) != null) {
            input = invocation.getAttachment(INPUT_KEY);
        }
        if (result != null && result.getAttachment(OUTPUT_KEY) != null) {
            output = result.getAttachment(OUTPUT_KEY);
        }

        return new ServiceConfigURL(COUNT_PROTOCOL, NetUtils.getLocalHost(), localPort,
            service + PATH_SEPARATOR + method,
            APPLICATION_KEY, application,
            INTERFACE_KEY, service,
            METHOD_KEY, method,
            remoteKey, remoteValue,
            error ? FAILURE_KEY : SUCCESS_KEY, "1",
            ELAPSED_KEY, String.valueOf(elapsed),
            CONCURRENT_KEY, String.valueOf(concurrent),
            INPUT_KEY, input,
            OUTPUT_KEY, output,
            GROUP_KEY, group,
            VERSION_KEY, version);
    }


}
