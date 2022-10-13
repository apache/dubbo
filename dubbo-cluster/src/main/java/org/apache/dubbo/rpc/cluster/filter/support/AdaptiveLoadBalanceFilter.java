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
package org.apache.dubbo.rpc.cluster.filter.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.profiler.Profiler;
import org.apache.dubbo.common.profiler.ProfilerEntry;
import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;
import org.apache.dubbo.rpc.cluster.loadbalance.AdaptiveLoadBalance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.*;

/**
 * if the load balance is adaptive ,set attachment to get the metrics of the server
 * @see Filter
 * @see RpcContext
 */
@Activate(group = CONSUMER, order = -200000)
public class AdaptiveLoadBalanceFilter implements ClusterFilter, ClusterFilter.Listener {

    /**
     * uses a single worker thread operating off an bounded queue
     */
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1024),
            new NamedInternalThreadFactory("Dubbo-framework-loadbalance-adaptive", true), new ThreadPoolExecutor.DiscardOldestPolicy());

    public AdaptiveLoadBalanceFilter(ApplicationModel applicationModel) {
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    private String buildServiceKey(Invocation invocation){
        return invocation.getInvoker().getUrl().getAddress() + ":" + invocation.getProtocolServiceKey();
        //return url.getAddress() + ProtocolUtils.serviceKey(url.getPort(), url.getPath(), url.getVersion(), url.getGroup());
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {

        try {
            if (StringUtils.isNotEmpty(invoker.getUrl().getParameter(LOADBALANCE_KEY))
                && AdaptiveLoadBalance.NAME.equals(invoker.getUrl().getParameter(LOADBALANCE_KEY))) {
                AdaptiveMetrics.addConsumerSuccess(buildServiceKey(invocation));
            }
            String attachment = appResponse.getAttachment(Constants.ADAPTIVE_LOADBALANCE_ATTACHMENT_KEY);
            if (StringUtils.isNotEmpty(attachment)) {
                String[] parties = COMMA_SPLIT_PATTERN.split(attachment);
                if (parties.length == 0) {
                    return;
                }
                Map<String, String> metricsMap = new HashMap<>();
                for (String party : parties) {
                    String[] groups = party.split(":");
                    if (groups.length != 2) {
                        continue;
                    }
                    metricsMap.put(groups[0], groups[1]);
                }
                ProfilerEntry profilerEntry = (ProfilerEntry) invocation.getAttributes().get(Profiler.PROFILER_KEY);
                long rt = (System.nanoTime() - profilerEntry.getStartTime()) / 1000_000L;
                metricsMap.put("rt", String.valueOf(rt));

                executor.execute(() -> {
                    AdaptiveMetrics.setProviderMetrics(buildServiceKey(invocation), metricsMap);
                });
            }
        }
        finally {
            appResponse.getAttachments().remove(Constants.ADAPTIVE_LOADBALANCE_ATTACHMENT_KEY);
        }

    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        executor.execute(() -> {
            AdaptiveMetrics.addErrorReq(buildServiceKey(invocation));
        });
    }


}
