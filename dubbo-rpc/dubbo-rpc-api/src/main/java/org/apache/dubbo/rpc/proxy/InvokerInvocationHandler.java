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
package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.profiler.Profiler;
import org.apache.dubbo.common.profiler.ProfilerEntry;
import org.apache.dubbo.common.profiler.ProfilerSwitch;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ServiceModel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;

/**
 * InvokerHandler
 */
public class InvokerInvocationHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);
    private final Invoker<?> invoker;
    private ServiceModel serviceModel;
    private URL url;
    private String protocolServiceKey;

    public static Field stackTraceField;

    static {
        try {
            stackTraceField = Throwable.class.getDeclaredField("stackTrace");
            stackTraceField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // ignore
        }
    }

    public InvokerInvocationHandler(Invoker<?> handler) {
        this.invoker = handler;
        this.url = invoker.getUrl();
        this.protocolServiceKey = this.url.getProtocolServiceKey();
        this.serviceModel = this.url.getServiceModel();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(invoker, args);
        }
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            if ("toString".equals(methodName)) {
                return invoker.toString();
            } else if ("$destroy".equals(methodName)) {
                invoker.destroy();
                return null;
            } else if ("hashCode".equals(methodName)) {
                return invoker.hashCode();
            }
        } else if (parameterTypes.length == 1 && "equals".equals(methodName)) {
            return invoker.equals(args[0]);
        }
        RpcInvocation rpcInvocation = new RpcInvocation(serviceModel, method, invoker.getInterface().getName(), protocolServiceKey, args);
        String serviceKey = url.getServiceKey();
        rpcInvocation.setTargetServiceUniqueName(serviceKey);

        // invoker.getUrl() returns consumer url.
        RpcServiceContext.setRpcContext(url);

        if (serviceModel instanceof ConsumerModel) {
            rpcInvocation.put(Constants.CONSUMER_MODEL, serviceModel);
            rpcInvocation.put(Constants.METHOD_MODEL, ((ConsumerModel) serviceModel).getMethodModel(method));
        }

        if (ProfilerSwitch.isEnableProfiler()) {
            ProfilerEntry bizProfiler = Profiler.getBizProfiler();
            boolean containsBizProfiler = false;
            if (bizProfiler != null) {
                containsBizProfiler = true;
                ProfilerEntry currentNode = Profiler.enter(bizProfiler, "Receive request. Client invoke begin.");
                rpcInvocation.put(Profiler.PROFILER_KEY, currentNode);
            } else {
                bizProfiler = Profiler.start("Receive request. Client invoke begin.");
            }
            try {
                return invoker.invoke(rpcInvocation).recreate();
            } finally {
                Profiler.release(bizProfiler);
                if (!containsBizProfiler) {
                    int timeout;
                    Object timeoutKey = rpcInvocation.getObjectAttachment(TIMEOUT_KEY);
                    if (timeoutKey instanceof Integer) {
                        timeout = (Integer) timeoutKey;
                    } else {
                        timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
                    }
                    if (bizProfiler.getEndTime() - bizProfiler.getStartTime() > (timeout * ProfilerSwitch.getWarnPercent())) {
                        StringBuilder attachment = new StringBuilder();
                        for (Map.Entry<String, Object> entry : rpcInvocation.getObjectAttachments().entrySet()) {
                            attachment.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
                        }

                        logger.warn(String.format("[Dubbo-Consumer] execute service %s#%s cost %d ms, this invocation almost (maybe already) timeout\n" +
                                "invocation context:\n %s\n" +
                                "thread info: \n%s",
                            protocolServiceKey, method, bizProfiler.getEndTime() - bizProfiler.getStartTime(),
                            attachment, Profiler.buildDetail(bizProfiler)));
                    }
                }
            }
        }

        return invoker.invoke(rpcInvocation).recreate();
    }
}
