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
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.profiler.Profiler;
import org.apache.dubbo.common.profiler.ProfilerEntry;
import org.apache.dubbo.common.profiler.ProfilerSwitch;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROXY_TIMEOUT_RESPONSE;

@Activate(group = PROVIDER, order = Integer.MIN_VALUE)
public class ProfilerServerFilter implements Filter, BaseFilter.Listener {
    private final static String CLIENT_IP_KEY = "client_ip";
    private final static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ProfilerServerFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (ProfilerSwitch.isEnableSimpleProfiler()) {
            ProfilerEntry bizProfiler;
            Object localInvokeProfiler = invocation.get(Profiler.PROFILER_KEY);
            if (localInvokeProfiler instanceof ProfilerEntry) {
                bizProfiler = Profiler.enter((ProfilerEntry) localInvokeProfiler, "Receive request. Local server invoke begin.");
            } else {
                bizProfiler = Profiler.start("Receive request. Server invoke begin.");
            }
            invocation.put(Profiler.PROFILER_KEY, bizProfiler);
            invocation.put(CLIENT_IP_KEY, RpcContext.getServiceContext().getRemoteAddressString());
        }

        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        afterInvoke(invoker, invocation);
        addAdaptiveResponse(appResponse, invocation);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        afterInvoke(invoker, invocation);
    }

    private void afterInvoke(Invoker<?> invoker, Invocation invocation) {
        if (ProfilerSwitch.isEnableSimpleProfiler()) {
            Object fromInvocation = invocation.get(Profiler.PROFILER_KEY);
            if (fromInvocation instanceof ProfilerEntry) {
                ProfilerEntry profiler = Profiler.release((ProfilerEntry) fromInvocation);
                invocation.put(Profiler.PROFILER_KEY, profiler);

                dumpIfNeed(invoker, invocation, (ProfilerEntry) fromInvocation);
            }
        }
    }
    private void addAdaptiveResponse(Result appResponse, Invocation invocation) {
        String adaptiveLoadAttachment = invocation.getAttachment(Constants.ADAPTIVE_LOADBALANCE_ATTACHMENT_KEY);
        if (StringUtils.isNotEmpty(adaptiveLoadAttachment)) {
            OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

            StringBuilder sb = new StringBuilder(64);
            sb.append("curTime:").append(System.currentTimeMillis());
            sb.append(COMMA_SEPARATOR).append("load:").append(operatingSystemMXBean.getSystemLoadAverage() * 100 / operatingSystemMXBean.getAvailableProcessors() );

            appResponse.setAttachment(Constants.ADAPTIVE_LOADBALANCE_ATTACHMENT_KEY,sb.toString());
        }
    }

    private void dumpIfNeed(Invoker<?> invoker, Invocation invocation, ProfilerEntry profiler) {
        Long timeout = RpcUtils.convertToNumber(invocation.getObjectAttachmentWithoutConvert(TIMEOUT_KEY));

        if (timeout == null) {
            timeout = (long) invoker.getUrl().getMethodPositiveParameter(invocation.getMethodName(), TIMEOUT_KEY, DEFAULT_TIMEOUT);
        }
        long usage = profiler.getEndTime() - profiler.getStartTime();
        if (((usage / (1000_000L * ProfilerSwitch.getWarnPercent())) > timeout) && timeout != -1) {

            StringBuilder attachment = new StringBuilder();
            invocation.foreachAttachment((entry) -> {
                attachment.append(entry.getKey()).append("=").append(entry.getValue()).append(";\n");
            });

            logger.warn(PROXY_TIMEOUT_RESPONSE, "", "",
                String.format("[Dubbo-Provider] execute service %s#%s cost %d.%06d ms, this invocation almost (maybe already) timeout. Timeout: %dms\n" +
                        "client: %s\n" +
                        "invocation context:\n%s" +
                        "thread info: \n%s",
                    invocation.getTargetServiceUniqueName(), invocation.getMethodName(), usage / 1000_000, usage % 1000_000, timeout,
                    invocation.get(CLIENT_IP_KEY), attachment, Profiler.buildDetail(profiler)));
        }
    }
}
