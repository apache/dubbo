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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.*;

/**
 * AdaptiveLoadBalance
 * </p>
 */
public class AdaptiveLoadBalance extends AbstractLoadBalance implements ScopeModelAware {

    public static final String NAME = "adaptive";

    //default key
    private String attachmentKey = "mem,load";

    private final int default_timeout = 30_000;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
    }

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        Invoker invoker = selectByP2C(invokers,url,invocation);
        invocation.setAttachment(Constants.ADAPTIVE_LOADBALANCE_ATTACHMENT_KEY,attachmentKey);
        AdaptiveMetrics.addConsumerReq(buildServiceKey(invoker,invocation));
        AdaptiveMetrics.setPickTime(buildServiceKey(invoker,invocation),System.currentTimeMillis());

        return invoker;
    }

    private <T> Invoker<T> selectByP2C(List<Invoker<T>> invokers, URL url, Invocation invocation){
        int length = invokers.size();
        if(length == 1) {
            return invokers.get(0);
        }

        if(length == 2) {
            return chooseLowLoadInvoker(invokers.get(0),invokers.get(1),invocation);
        }

        int pos1 = ThreadLocalRandom.current().nextInt(length);
        int pos2 = ThreadLocalRandom.current().nextInt(length);
        while(pos1 == pos2) {
            pos2 = ThreadLocalRandom.current().nextInt(length);
        }

        return chooseLowLoadInvoker(invokers.get(pos1),invokers.get(pos2),invocation);
    }

    private String buildServiceKey(Invoker<?> invoker,Invocation invocation){
        URL url = invoker.getUrl();
        return url.getAddress() + ":" + invocation.getProtocolServiceKey();
    }

    private int getTimeout(Invoker<?> invoker, Invocation invocation) {
        URL url = invoker.getUrl();
        Object countdown = RpcContext.getClientAttachment().getObjectAttachment(TIME_COUNTDOWN_KEY);
        int timeout;
        if (countdown == null) {
            timeout = (int) RpcUtils.getTimeout(url, invocation.getMethodName(), RpcContext.getClientAttachment(), DEFAULT_TIMEOUT);
        } else {
            TimeoutCountDown timeoutCountDown = (TimeoutCountDown) countdown;
            timeout = (int) timeoutCountDown.timeRemaining(TimeUnit.MILLISECONDS);
        }
        return timeout;
    }

    private <T> Invoker<T> chooseLowLoadInvoker(Invoker<T> invoker1,Invoker<T> invoker2,Invocation invocation){
        int weight1 = getWeight(invoker1, invocation);
        int weight2 = getWeight(invoker2, invocation);
        int timeout1 = getTimeout(invoker2, invocation);
        int timeout2 = getTimeout(invoker2, invocation);
        long load1 = Double.doubleToLongBits(AdaptiveMetrics.getLoad(buildServiceKey(invoker1,invocation),weight1,timeout1 ));
        long load2 = Double.doubleToLongBits(AdaptiveMetrics.getLoad(buildServiceKey(invoker2,invocation),weight2,timeout2 ));

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
