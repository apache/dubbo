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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.LoadbalanceRules;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.AdaptiveMetrics;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;

/**
 * AdaptiveLoadBalance
 * </p>
 */
public class AdaptiveLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "adaptive";

    //default key
    private String attachmentKey = "mem,load";

    private final AdaptiveMetrics adaptiveMetrics;

    public AdaptiveLoadBalance(ApplicationModel scopeModel){
        adaptiveMetrics = scopeModel.getBeanFactory().getBean(AdaptiveMetrics.class);
    }

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        Invoker<T> invoker = selectByP2C(invokers,invocation);
        invocation.setAttachment(Constants.ADAPTIVE_LOADBALANCE_ATTACHMENT_KEY,attachmentKey);
        long startTime = System.currentTimeMillis();
        invocation.getAttributes().put(Constants.ADAPTIVE_LOADBALANCE_START_TIME,startTime);
        invocation.getAttributes().put(LOADBALANCE_KEY,LoadbalanceRules.ADAPTIVE);
        adaptiveMetrics.addConsumerReq(getServiceKey(invoker,invocation));
        adaptiveMetrics.setPickTime(getServiceKey(invoker,invocation),startTime);

        return invoker;
    }

    private <T> Invoker<T> selectByP2C(List<Invoker<T>> invokers, Invocation invocation){
        int length = invokers.size();
        if(length == 1) {
            return invokers.get(0);
        }

        if(length == 2) {
            return chooseLowLoadInvoker(invokers.get(0),invokers.get(1),invocation);
        }

        int pos1 = ThreadLocalRandom.current().nextInt(length);
        int pos2 = ThreadLocalRandom.current().nextInt(length - 1);
        if (pos2 >= pos1) {
            pos2 = pos2 + 1;
        }

        return chooseLowLoadInvoker(invokers.get(pos1),invokers.get(pos2),invocation);
    }

    private String getServiceKey(Invoker<?> invoker,Invocation invocation){

        String key = (String) invocation.getAttributes().get(invoker);
        if (StringUtils.isNotEmpty(key)){
            return key;
        }

        key = buildServiceKey(invoker,invocation);
        invocation.getAttributes().put(invoker,key);
        return key;
    }

    private String buildServiceKey(Invoker<?> invoker,Invocation invocation){
        URL url = invoker.getUrl();
        StringBuilder sb = new StringBuilder(128);
        sb.append(url.getAddress()).append(":").append(invocation.getProtocolServiceKey());
        return sb.toString();
    }

    private int getTimeout(Invoker<?> invoker, Invocation invocation) {
        URL url = invoker.getUrl();
        String methodName = RpcUtils.getMethodName(invocation);
        return (int) RpcUtils.getTimeout(url,methodName, RpcContext.getClientAttachment(),invocation, DEFAULT_TIMEOUT);
    }

    private <T> Invoker<T> chooseLowLoadInvoker(Invoker<T> invoker1,Invoker<T> invoker2,Invocation invocation){
        int weight1 = getWeight(invoker1, invocation);
        int weight2 = getWeight(invoker2, invocation);
        int timeout1 = getTimeout(invoker2, invocation);
        int timeout2 = getTimeout(invoker2, invocation);
        long load1 = Double.doubleToLongBits(adaptiveMetrics.getLoad(getServiceKey(invoker1,invocation),weight1,timeout1 ));
        long load2 = Double.doubleToLongBits(adaptiveMetrics.getLoad(getServiceKey(invoker2,invocation),weight2,timeout2 ));

        if (load1 == load2) {
            // The sum of weights
            int totalWeight = weight1 + weight2;
            if (totalWeight > 0) {
                int offset = ThreadLocalRandom.current().nextInt(totalWeight);
                if (offset < weight1) {
                    return invoker1;
                }
                return invoker2;
            }
            return ThreadLocalRandom.current().nextInt(2) == 0 ? invoker1 : invoker2;
        }
        return load1 > load2 ? invoker2 : invoker1;
    }

}
