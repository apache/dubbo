/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * 带有权重与warmup功能的哈希负载均衡策略
 *
 * 权重特性实现方式：
 *          一致性哈希的负载策略，跟据权重计算每个服务提供者的虚拟哈希节点的数量
 *  warmup特性的实现方式：
 *          在warmup期间，定时调整服务提供者的权重，并重新计算虚拟哈希节点的数量，并重建哈希环
 *          因为重建哈希是一个重量级操作，所以warmup期间不是每次请求都重建哈希，而是每隔一分钟调整一次
 *
 * 注意在warmup此负载策略不具有sticky特性
 * 注意默认使用第一个方法的参数作为哈希分片基准，如果服务方法没有参数，则所有的请求流到同一个节点，权重与warmup特性失效
 *
 * Created by wujianchao on 2017/6/7.
 */
public class WeightedConsistentHashLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "weightedconsistenthash";

    private static final Logger logger = LoggerFactory.getLogger(WeightedConsistentHashLoadBalance.class);

    private final ConcurrentMap<String, WeightedConsistentHashSelector<?>> selectors = new ConcurrentHashMap<String, WeightedConsistentHashSelector<?>>();

    @SuppressWarnings("unchecked")
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        int identityHashCode = System.identityHashCode(invokers);
        WeightedConsistentHashSelector<T> selector = (WeightedConsistentHashSelector<T>) selectors.get(key);
        if (selector == null) {
            //发现新的服务类型，构建哈希
            selector = new WeightedConsistentHashSelector<T>(invokers, invocation, identityHashCode);
            selectors.put(key, selector);
        } else if (selector.getIdentityHashCode() != identityHashCode) {
            //服务提供者有变动，比如调整权重，必须重新构建WeightedConsistentHashSelector
            logger.debug("Rebuild hash of service \"" + key + "\", for some of its providers change.");
            selector = new WeightedConsistentHashSelector<T>(invokers, invocation, identityHashCode);
            selectors.put(key, selector);
        }
        if (selector.isTimeToRebuildConsistentHash(invokers)) {
            //服务提供者warnup期间，定时重建哈希，以达到缓慢增加权重的目的
            logger.debug("Rebuild hash of service \"" + key + "\", for it is in warmup period.");
            selector.buildConsistentHash(invokers, invocation);
        }

        return selector.select(invokers, invocation);
    }

    /**
     * 默认使用第一个方法的参数作为哈希分片基准
     * 注意如果服务方法没有参数，则所有的请求流到同一个节点
     */
    private final class WeightedConsistentHashSelector<T> {

        private TreeMap<Long, Invoker<T>> virtualInvokers;

        private final int replicaNumber;

        private final int identityHashCode;

        private final int[] argumentIndex;

        //Make sure at least once rebuilding.
        //When there is no request in a long time, no rebuilding will be triggered.
        private AtomicBoolean isFirstStable = new AtomicBoolean(false);

        //Next rebuild time
        private AtomicLong nextwarmupTime = new AtomicLong(System.currentTimeMillis() + WARMUPPERIOD);

        //Every WARMUPPERIOD time rebuild hash in warmup period.
        private static final int WARMUPPERIOD = 60 * 1000;

        public WeightedConsistentHashSelector(List<Invoker<T>> invokers, Invocation invocation, int identityHashCode) {
            String methodName = invocation.getMethodName();
            this.virtualInvokers = new TreeMap<Long, Invoker<T>>();
            this.identityHashCode = identityHashCode;
            URL url = invokers.get(0).getUrl();
            this.replicaNumber = url.getMethodParameter(methodName, "hash.nodes", 160);
            String[] index = Constants.COMMA_SPLIT_PATTERN.split(url.getMethodParameter(methodName, "hash.arguments", "0"));
            argumentIndex = new int[index.length];
            for (int i = 0; i < index.length; i++) {
                argumentIndex[i] = Integer.parseInt(index[i]);
            }

            buildConsistentHash(invokers, invocation);
        }

        /**
         * 根据服务提供者的权重设置虚拟节点数量，并以此来构建一致性哈希
         * @param invokers 服务提供者
         * @param invocation 一次RPC调用信息
         */
        private void buildConsistentHash(List<Invoker<T>> invokers, Invocation invocation) {
            // 总权重
            int totalWeight = 0;
            for (int i = 0; i < invokers.size(); i++) {
                int weight = getWeight(invokers.get(i), invocation);
                totalWeight += weight;
            }

            //根据权重计算每个invoker对应一致性哈希环上的虚拟节点的个数
            int totalVirtualInvokerSize = replicaNumber * invokers.size();
            Map<Invoker<T>, Integer> virtualInvokerSize = new HashMap<Invoker<T>, Integer>();
            for (Invoker<T> invoker : invokers) {
                int size = (int) ((float) getWeight(invoker, invocation) / (float) totalWeight * (float) totalVirtualInvokerSize);
                size = size == 0 ? 1 : size;
                virtualInvokerSize.put(invoker, size);
            }

            //根据每个invoker的虚拟节点数量构建一致性哈希
            //注意这里会重新构建哈希，是重量级操作
            TreeMap<Long, Invoker<T>> newVirtualInvokers = new TreeMap<Long, Invoker<T>>();
            for (Map.Entry<Invoker<T>, Integer> entry : virtualInvokerSize.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    byte[] digest = md5(entry.getKey().getUrl().toFullString() + i);
                    long m = hash(digest, i % 4);
                    newVirtualInvokers.put(m, entry.getKey());
                }
            }

            //更新状态采用引用替换的方式，规避同步问题
            virtualInvokers = newVirtualInvokers;

            //打印新哈希信息
            StringBuffer hashInfo = new StringBuffer();
            if(logger.isDebugEnabled()){
                for (Map.Entry<Invoker<T>, Integer> entry : virtualInvokerSize.entrySet()) {
                    hashInfo.append("\n")
                            .append(entry.getKey().getUrl().getAddress().toString())
                            .append("=").append(entry.getValue());
                }
                logger.debug("Hash virtual nodes distribution info : " + hashInfo.toString());

            }
        }

        /**
         * 服务启动预热功能，设计上每个提供者（invoker）会在warmup时间内动态调整自己的权重
         * 因为构建哈希环是重操作，所以不会每次请求都构建一次哈希，采用定时构建的方式
         */
        private <T> boolean isTimeToRebuildConsistentHash(List<Invoker<T>> invokers) {
            if(invokers.size() == 1){
                return false;
            }
            long current = System.currentTimeMillis();
            boolean someoneInWarnupTime = false;
            for (Invoker<T> invoker : invokers) {
                long startup = invoker.getUrl().getParameter(Constants.REMOTE_TIMESTAMP_KEY, 0L);
                long warmup = invoker.getUrl().getParameter(Constants.WARMUP_KEY, Constants.DEFAULT_WARMUP);
                if (current < startup + warmup) {
                    someoneInWarnupTime = true;
                    break;
                }
            }

            //Make sure at least once rebuilding.
            //When there is no request in a long time, no rebuilding will be triggered.
            if(!someoneInWarnupTime){
                if(isFirstStable.compareAndSet(false, true)){
                    return true;
                }
            }

            if(someoneInWarnupTime && current >= nextwarmupTime.get()){
                synchronized (nextwarmupTime){
                    if(someoneInWarnupTime && current >= nextwarmupTime.get()){
                        nextwarmupTime.getAndAdd(WARMUPPERIOD);
                        return true;
                    }
                }
            }

            return false;
        }

        public int getIdentityHashCode() {
            return identityHashCode;
        }

        public Invoker<T> select(List<Invoker<T>> invokers, Invocation invocation) {
            String key = toKey(invocation.getArguments());
            byte[] digest = md5(key);
            Invoker<T> invoker = selectForKey(hash(digest, 0));
            return invoker;
        }

        private String toKey(Object[] args) {
            StringBuilder buf = new StringBuilder();
            for (int i : argumentIndex) {
                if (i >= 0 && i < args.length) {
                    buf.append(args[i]);
                }
            }
            return buf.toString();
        }

        private Invoker<T> selectForKey(long hash) {
            TreeMap<Long, Invoker<T>> consistentHash = virtualInvokers;
            Invoker<T> invoker;
            Long key = hash;
            if (!consistentHash.containsKey(key)) {
                SortedMap<Long, Invoker<T>> tailMap = consistentHash.tailMap(key);
                if (tailMap.isEmpty()) {
                    key = consistentHash.firstKey();
                } else {
                    key = tailMap.firstKey();
                }
            }
            invoker = consistentHash.get(key);
            return invoker;
        }

        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[0 + number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }

        private byte[] md5(String value) {
            MessageDigest md5;
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            md5.reset();
            byte[] bytes = null;
            try {
                bytes = value.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            md5.update(bytes);
            return md5.digest();
        }

    }

}
