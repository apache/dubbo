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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.profiler.Profiler;
import org.apache.dubbo.common.profiler.ProfilerEntry;
import org.apache.dubbo.common.profiler.ProfilerSwitch;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;

@Activate(group = PROVIDER, order = Integer.MAX_VALUE)
public class ProfilerServerFilter implements Filter, BaseFilter.Listener {
    private final static Logger logger = LoggerFactory.getLogger(ProfilerServerFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (ProfilerSwitch.isEnableProfiler()) {
            ProfilerEntry bizProfiler = Profiler.start("Receive request. Server invoke begin.");
            invocation.put(Profiler.PROFILER_KEY, bizProfiler);
        }

        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        if (ProfilerSwitch.isEnableProfiler()) {
            Object fromInvocation = invocation.get(Profiler.PROFILER_KEY);
            if (fromInvocation instanceof ProfilerEntry) {
                ProfilerEntry profiler = Profiler.release((ProfilerEntry) fromInvocation);
                invocation.put(Profiler.PROFILER_KEY, profiler);

                int timeout;
                Object timeoutKey = invocation.getObjectAttachment(TIMEOUT_KEY);
                if (timeoutKey instanceof Integer) {
                    timeout = (Integer) timeoutKey;
                } else {
                    timeout = invoker.getUrl().getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
                }
                if (profiler.getEndTime() - profiler.getStartTime() > (timeout * ProfilerSwitch.getWarnPercent())) {

                    StringBuilder attachment = new StringBuilder();
                    for (Map.Entry<String, Object> entry : invocation.getObjectAttachments().entrySet()) {
                        attachment.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
                    }

                    logger.warn(String.format("[Dubbo-Provider] execute service %s#%s cost %d ms, this invocation almost (maybe already) timeout\n" +
                            "client: %s\n" +
                            "invocation context:\n %s\n" +
                            "thread info: \n%s",
                        invocation.getProtocolServiceKey(), invocation.getMethodName(), profiler.getEndTime() - profiler.getStartTime(),
                        RpcContext.getServiceContext().getRemoteHost(), attachment, Profiler.buildDetail(profiler)));
                }
            }
        }
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        if (ProfilerSwitch.isEnableProfiler()) {
            Object fromInvocation = invocation.get(Profiler.PROFILER_KEY);
            if (fromInvocation instanceof ProfilerEntry) {
                invocation.put(Profiler.PROFILER_KEY, Profiler.release((ProfilerEntry) fromInvocation));
                logger.info(Profiler.buildDetail((ProfilerEntry) fromInvocation));
            }
        }
    }
}
