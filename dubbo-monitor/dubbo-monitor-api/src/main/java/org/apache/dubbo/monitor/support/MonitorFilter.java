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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.monitor.Monitor;
import org.apache.dubbo.monitor.MonitorFactory;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ListenableFilter;
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
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.monitor.Constants.COUNT_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.rpc.Constants.INPUT_KEY;
import static org.apache.dubbo.rpc.Constants.OUTPUT_KEY;
/**
 * MonitorFilter. (SPI, Singleton, ThreadSafe)
 */
@Activate(group = {PROVIDER, CONSUMER})
public class MonitorFilter extends ListenableFilter {

    private static final Logger logger = LoggerFactory.getLogger(MonitorFilter.class);
    private static final String MONITOR_FILTER_START_TIME = "monitor_filter_start_time";

    public MonitorFilter() {
        super.listener = new MonitorListener();
    }
    /**
     * The Concurrent counter
     */
    private final ConcurrentMap<String, AtomicInteger> concurrents = new ConcurrentHashMap<String, AtomicInteger>();

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
        if (invoker.getUrl().hasParameter(MONITOR_KEY)) {
            invocation.setAttachment(MONITOR_FILTER_START_TIME, String.valueOf(System.currentTimeMillis()));
            getConcurrent(invoker, invocation).incrementAndGet(); // count up
        }
        return invoker.invoke(invocation); // proceed invocation chain
    }

    // concurrent counter
    private AtomicInteger getConcurrent(Invoker<?> invoker, Invocation invocation) {
        String key = invoker.getInterface().getName() + "." + invocation.getMethodName();
        AtomicInteger concurrent = concurrents.get(key);
        if (concurrent == null) {
            concurrents.putIfAbsent(key, new AtomicInteger());
            concurrent = concurrents.get(key);
        }
        return concurrent;
    }

    class MonitorListener implements Listener {

        @Override
        public void onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
            if (invoker.getUrl().hasParameter(MONITOR_KEY)) {
                collect(invoker, invocation, result, RpcContext.getContext().getRemoteHost(), Long.valueOf(invocation.getAttachment(MONITOR_FILTER_START_TIME)), false);
                getConcurrent(invoker, invocation).decrementAndGet(); // count down
            }
        }

        @Override
        public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
            if (invoker.getUrl().hasParameter(MONITOR_KEY)) {
                collect(invoker, invocation, null, RpcContext.getContext().getRemoteHost(), Long.valueOf(invocation.getAttachment(MONITOR_FILTER_START_TIME)), true);
                getConcurrent(invoker, invocation).decrementAndGet(); // count down
            }
        }

        /**
         * The collector logic, it will be handled by the default monitor
         *
         * @param invoker
         * @param invocation
         * @param result     the invoke result
         * @param remoteHost the remote host address
         * @param start      the timestamp the invoke begin
         * @param error      if there is an error on the invoke
         */
        private void collect(Invoker<?> invoker, Invocation invocation, Result result, String remoteHost, long start, boolean error) {
            try {
                URL monitorUrl = invoker.getUrl().getUrlParameter(MONITOR_KEY);
                Monitor monitor = monitorFactory.getMonitor(monitorUrl);
                if (monitor == null) {
                    return;
                }
                URL statisticsURL = createStatisticsUrl(invoker, invocation, result, remoteHost, start, error);
                monitor.collect(statisticsURL);
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
            long elapsed = System.currentTimeMillis() - start; // invocation cost
            int concurrent = getConcurrent(invoker, invocation).get(); // current concurrent count
            String application = invoker.getUrl().getParameter(APPLICATION_KEY);
            String service = invoker.getInterface().getName(); // service name
            String method = RpcUtils.getMethodName(invocation); // method name
            String group = invoker.getUrl().getParameter(GROUP_KEY);
            String version = invoker.getUrl().getParameter(VERSION_KEY);

            int localPort;
            String remoteKey, remoteValue;
            if (CONSUMER_SIDE.equals(invoker.getUrl().getParameter(SIDE_KEY))) {
                // ---- for service consumer ----
                localPort = 0;
                remoteKey = MonitorService.PROVIDER;
                remoteValue = invoker.getUrl().getAddress();
            } else {
                // ---- for service provider ----
                localPort = invoker.getUrl().getPort();
                remoteKey = MonitorService.CONSUMER;
                remoteValue = remoteHost;
            }
            String input = "", output = "";
            if (invocation.getAttachment(INPUT_KEY) != null) {
                input = invocation.getAttachment(INPUT_KEY);
            }
            if (result != null && result.getAttachment(OUTPUT_KEY) != null) {
                output = result.getAttachment(OUTPUT_KEY);
            }

            return new URL(COUNT_PROTOCOL, NetUtils.getLocalHost(), localPort, service + PATH_SEPARATOR + method, MonitorService.APPLICATION, application, MonitorService.INTERFACE, service, MonitorService.METHOD, method, remoteKey, remoteValue, error ? MonitorService.FAILURE : MonitorService.SUCCESS, "1", MonitorService.ELAPSED, String.valueOf(elapsed), MonitorService.CONCURRENT, String.valueOf(concurrent), INPUT_KEY, input, OUTPUT_KEY, output, GROUP_KEY, group, VERSION_KEY, version);
        }

    }

}
