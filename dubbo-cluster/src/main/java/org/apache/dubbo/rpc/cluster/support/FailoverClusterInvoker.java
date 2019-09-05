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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * When invoke fails, log the initial error and retry other invokers (retry n times, which means at most n different invokers will be invoked)
 * Note that retry causes latency.
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Failover">Failover</a>
 *
 */
public class FailoverClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailoverClusterInvoker.class);

    public FailoverClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result doInvoke(Invocation invocation, final List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        List<Invoker<T>> copyInvokers = invokers;//服务提供者列表
        checkInvokers(copyInvokers, invocation);//检查消息提供者列表是否为空
        String methodName = RpcUtils.getMethodName(invocation);//调用方法
        int len = getUrl().getMethodParameter(methodName, Constants.RETRIES_KEY, Constants.DEFAULT_RETRIES) + 1;
        if (len <= 0) {//获取重试次数，如果重试次数配置<=0，这默认执行一次。
            len = 1;
        }
        // retry loop.
        RpcException le = null; // last exception.
        // 构造调用过的缓存List，用于重试，只要调用过则添加到list中
        List<Invoker<T>> invoked = new ArrayList<Invoker<T>>(copyInvokers.size());
        Set<String> providers = new HashSet<String>(len);
        for (int i = 0; i < len; i++) {
            if (i > 0) {//如果不是第一次调用，则证明开始了重试，则证明第一次调用失败了
                checkWhetherDestroyed();//只要是重试每次都要检查invoker列表是否有被销毁
                copyInvokers = list(invocation);//重新筛选invoker列表
                checkInvokers(copyInvokers, invocation);//再次检查copyInvokers是否为空
            }
            //从多个服务提供者列表中，选择其中一个invoker。==>>所谓的负载策略
            Invoker<T> invoker = select(loadbalance, invocation, copyInvokers, invoked);
            invoked.add(invoker);//选择出的证明接下来要用的，直接添加到已调用列表
            RpcContext.getContext().setInvokers((List) invoked);
            try {
                return invoker.invoke(invocation);//触发RPC调用
            } catch (RpcException e) {
                //异常处理
            } finally {
                providers.add(invoker.getUrl().getAddress());
            }
        }
        throw new RpcException("所有invoker都调用完或重试完，还没有成功的，直接抛RpcException");
    }

}
