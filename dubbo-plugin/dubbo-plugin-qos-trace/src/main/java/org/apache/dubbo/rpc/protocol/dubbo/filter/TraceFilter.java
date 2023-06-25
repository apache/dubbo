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
package org.apache.dubbo.rpc.protocol.dubbo.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_PARSE;

/**
 * TraceFilter
 */
@Activate(group = CommonConstants.PROVIDER)
public class TraceFilter implements Filter {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(TraceFilter.class);

    private static final String TRACE_MAX = "trace.max";

    private static final String TRACE_COUNT = "trace.count";

    private static final ConcurrentMap<String, Set<Channel>> TRACERS = new ConcurrentHashMap<>();

    public static void addTracer(Class<?> type, String method, Channel channel, int max) {
        channel.setAttribute(TRACE_MAX, max);
        channel.setAttribute(TRACE_COUNT, new AtomicInteger());
        String key = StringUtils.isNotEmpty(method) ? type.getName() + "." + method : type.getName();
        Set<Channel> channels = TRACERS.computeIfAbsent(key, k -> new ConcurrentHashSet<>());
        channels.add(channel);
    }

    public static void removeTracer(Class<?> type, String method, Channel channel) {
        channel.removeAttribute(TRACE_MAX);
        channel.removeAttribute(TRACE_COUNT);
        String key = StringUtils.isNotEmpty(method) ? type.getName() + "." + method : type.getName();
        Set<Channel> channels = TRACERS.get(key);
        if (channels != null) {
            channels.remove(channel);
        }
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long start = System.currentTimeMillis();
        Result result = invoker.invoke(invocation);
        long end = System.currentTimeMillis();
        if (TRACERS.size() > 0) {
            String key = invoker.getInterface().getName() + "." + invocation.getMethodName();
            Set<Channel> channels = TRACERS.get(key);
            if (CollectionUtils.isEmpty(channels)) {
                key = invoker.getInterface().getName();
                channels = TRACERS.get(key);
            }
            if (CollectionUtils.isNotEmpty(channels)) {
                for (Channel channel : new ArrayList<>(channels)) {
                    if (channel.isConnected()) {
                        try {
                            int max = 1;
                            Integer m = (Integer) channel.getAttribute(TRACE_MAX);
                            if (m != null) {
                                max = m;
                            }
                            int count;
                            AtomicInteger c = (AtomicInteger) channel.getAttribute(TRACE_COUNT);
                            if (c == null) {
                                c = new AtomicInteger();
                                channel.setAttribute(TRACE_COUNT, c);
                            }
                            count = c.getAndIncrement();
                            if (count < max) {
                                String prompt = channel.getUrl().getParameter(Constants.PROMPT_KEY, Constants.DEFAULT_PROMPT);
                                channel.send("\r\n" + RpcContext.getServiceContext().getRemoteAddress() + " -> "
                                    + invoker.getInterface().getName()
                                    + "." + invocation.getMethodName()
                                    + "(" + JsonUtils.toJson(invocation.getArguments()) + ")" + " -> " + JsonUtils.toJson(result.getValue())
                                    + "\r\nelapsed: " + (end - start) + " ms."
                                    + "\r\n\r\n" + prompt);
                            }
                            if (count >= max - 1) {
                                channels.remove(channel);
                            }
                        } catch (Throwable e) {
                            channels.remove(channel);
                            logger.warn(PROTOCOL_FAILED_PARSE, "", "", e.getMessage(), e);
                        }
                    } else {
                        channels.remove(channel);
                    }
                }
            }
        }
        return result;
    }

}
