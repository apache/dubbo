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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.profiler.Profiler;
import org.apache.dubbo.common.profiler.ProfilerEntry;
import org.apache.dubbo.common.profiler.ProfilerSwitch;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.apache.dubbo.rpc.support.RpcUtils;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROXY_TIMEOUT_REQUEST;

public class InvocationUtil {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(InvokerInvocationHandler.class);

    public static Object invoke(Invoker<?> invoker, RpcInvocation rpcInvocation) throws Throwable {
        URL url = invoker.getUrl();
        String serviceKey = url.getServiceKey();
        rpcInvocation.setTargetServiceUniqueName(serviceKey);

        // invoker.getUrl() returns consumer url.
        RpcServiceContext.getServiceContext().setConsumerUrl(url);

        if (ProfilerSwitch.isEnableSimpleProfiler()) {
            ProfilerEntry parentProfiler = Profiler.getBizProfiler();
            ProfilerEntry bizProfiler;
            if (parentProfiler != null) {
                bizProfiler = Profiler.enter(parentProfiler,
                    "Receive request. Client invoke begin. ServiceKey: " + serviceKey + " MethodName:" + rpcInvocation.getMethodName());
            } else {
                bizProfiler = Profiler.start("Receive request. Client invoke begin. ServiceKey: " + serviceKey + " " + "MethodName:" + rpcInvocation.getMethodName());
            }
            rpcInvocation.put(Profiler.PROFILER_KEY, bizProfiler);
            try {
                return invoker.invoke(rpcInvocation).recreate();
            } finally {
                Profiler.release(bizProfiler);
                Long timeout = RpcUtils.convertToNumber(rpcInvocation.getObjectAttachmentWithoutConvert(TIMEOUT_KEY));

                if (timeout == null) {
                    timeout = (long) url.getMethodPositiveParameter(rpcInvocation.getMethodName(),
                        TIMEOUT_KEY,
                        DEFAULT_TIMEOUT);
                }
                long usage = bizProfiler.getEndTime() - bizProfiler.getStartTime();
                if ((usage / (1000_000L * ProfilerSwitch.getWarnPercent())) > timeout) {
                    StringBuilder attachment = new StringBuilder();
                    rpcInvocation.foreachAttachment((entry) -> {
                        attachment.append(entry.getKey()).append("=").append(entry.getValue()).append(";\n");
                    });

                    logger.warn(PROXY_TIMEOUT_REQUEST, "", "", String.format(
                        "[Dubbo-Consumer] execute service %s#%s cost %d.%06d ms, this invocation almost (maybe already) timeout. Timeout: %dms\n" + "invocation context:\n%s" + "thread info: \n%s",
                        rpcInvocation.getProtocolServiceKey(),
                        rpcInvocation.getMethodName(),
                        usage / 1000_000,
                        usage % 1000_000,
                        timeout,
                        attachment,
                        Profiler.buildDetail(bizProfiler)));
                }
            }
        }
        return invoker.invoke(rpcInvocation).recreate();
    }
}
