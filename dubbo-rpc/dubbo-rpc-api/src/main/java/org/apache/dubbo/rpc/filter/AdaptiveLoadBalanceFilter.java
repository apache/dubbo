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

import org.apache.dubbo.common.constants.LoadbalanceRules;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.resource.GlobalResourcesRepository;
import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.AdaptiveMetrics;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;

/**
 * if the load balance is adaptive ,set attachment to get the metrics of the server
 * @see org.apache.dubbo.rpc.Filter
 * @see org.apache.dubbo.rpc.RpcContext
 */
@Activate(group = CONSUMER, order = -200000, value = {"loadbalance:adaptive"})
public class AdaptiveLoadBalanceFilter implements Filter, Filter.Listener {

    /**
     * uses a single worker thread operating off an bounded queue
     */
    private volatile ThreadPoolExecutor executor = null;

    private final AdaptiveMetrics adaptiveMetrics;

    public AdaptiveLoadBalanceFilter(ApplicationModel scopeModel) {
        adaptiveMetrics = scopeModel.getBeanFactory().getBean(AdaptiveMetrics.class);
    }

    private ThreadPoolExecutor getExecutor(){
        if (null == executor) {
            synchronized (this) {
                if (null == executor) {
                    executor = new ThreadPoolExecutor(1, 1, 0L,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1024),
                        new NamedInternalThreadFactory("Dubbo-framework-loadbalance-adaptive", true), new ThreadPoolExecutor.DiscardOldestPolicy());
                    GlobalResourcesRepository.getInstance().registerDisposable(() -> this.executor.shutdown());
                }
            }
        }
        return executor;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    private String buildServiceKey(Invocation invocation){
        StringBuilder sb = new StringBuilder(128);
        sb.append(invocation.getInvoker().getUrl().getAddress()).append(":").append(invocation.getProtocolServiceKey());
        return sb.toString();
    }

    private String getServiceKey(Invocation invocation){

        String key = (String) invocation.getAttributes().get(invocation.getInvoker());
        if (StringUtils.isNotEmpty(key)){
            return key;
        }

        key = buildServiceKey(invocation);
        invocation.getAttributes().put(invocation.getInvoker(),key);
        return key;
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {

        try {
            String loadBalance = (String) invocation.getAttributes().get(LOADBALANCE_KEY);
            if (StringUtils.isEmpty(loadBalance)
                || !LoadbalanceRules.ADAPTIVE.equals(loadBalance)) {
                return;
            }
            adaptiveMetrics.addConsumerSuccess(getServiceKey(invocation));
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

                Long startTime = (Long) invocation.getAttributes().get(Constants.ADAPTIVE_LOADBALANCE_START_TIME);
                if (null != startTime) {
                    metricsMap.put("rt", String.valueOf(System.currentTimeMillis() - startTime));
                }

                getExecutor().execute(() -> {
                    adaptiveMetrics.setProviderMetrics(getServiceKey(invocation), metricsMap);
                });
            }
        }
        finally {
            appResponse.getAttachments().remove(Constants.ADAPTIVE_LOADBALANCE_ATTACHMENT_KEY);
        }

    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        String loadBalance = (String) invocation.getAttributes().get(LOADBALANCE_KEY);
        if (StringUtils.isNotEmpty(loadBalance)
            && LoadbalanceRules.ADAPTIVE.equals(loadBalance)) {
            getExecutor().execute(() -> {
                adaptiveMetrics.addErrorReq(getServiceKey(invocation));
            });
        }
    }


}
