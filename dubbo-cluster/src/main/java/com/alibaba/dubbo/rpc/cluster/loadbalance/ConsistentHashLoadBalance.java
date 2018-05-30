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
package com.alibaba.dubbo.rpc.cluster.loadbalance;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.support.RpcUtils;

/**
 * ConsistentHashLoadBalance
 * 
 * After fixed, the ConsistentHashLoadBalance will surpport to used in GenericService,
 * and it will reduce the times of rebuilding hash cycle when the provider restart 
 * but nothing to change
 * 
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    
    public static final String NAME = "consistenthash";

    private final ConcurrentMap<String, ConsistentHashSelector<?>> selectors = new ConcurrentHashMap<String, ConsistentHashSelector<?>>();

    @Override
    @SuppressWarnings("unchecked")
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        String methodName = RpcUtils.getMethodName(invocation);
        String key = invokers.get(0).getUrl().getServiceKey() + "." + methodName;    
        Map<String, Invoker<T>> invokerMap = new HashMap<String, Invoker<T>>();
        int identityHashCode = caculateInvokerHashCode(invokers, invokerMap);
        ConsistentHashSelector<T> selector = (ConsistentHashSelector<T>) selectors.get(key);
        if (selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(key, new ConsistentHashSelector<T>(invokers, methodName, identityHashCode));
            selector = (ConsistentHashSelector<T>) selectors.get(key);
        }
        return selector.select(invocation, invokerMap);
    }

    /**
     * Generate hashcode according to the invoker list.
     * the value only changes when adding or reducing the invoker.
     * 
     * The method use System.identityHashCode(invokers) to generate
     * hashcode in past.The result of method executed changes everytime,
     * because the timestamp of invoker alse be solved.
     * 
     * Now if the providers are only restart but nothing to change, 
     * the value also won't be changed;
     * @param invokers
     * @return the hashcode of invoker addresses
     */
    private <T> int caculateInvokerHashCode(List<Invoker<T>> invokers, Map<String, Invoker<T>> invokerMap) {
        StringBuilder metakey = new StringBuilder();
        for (Invoker<T> obj : invokers) {
            String address = obj.getUrl().getAddress();
            metakey.append(address);
            invokerMap.put(address, obj);
        }
        return metakey.toString().hashCode();
    }

    private static final class ConsistentHashSelector<T> {

        private final TreeMap<Long, String> virtualInvokers;

        private final int replicaNumber;

        private final int identityHashCode;

        private final int[] argumentIndex;

        ConsistentHashSelector(List<Invoker<T>> invokers, String methodName, int identityHashCode) {
            this.virtualInvokers = new TreeMap<Long, String>();
            this.identityHashCode = identityHashCode;
            URL url = invokers.get(0).getUrl();
            this.replicaNumber = url.getMethodParameter(methodName, "hash.nodes", 160);
            String[] index = Constants.COMMA_SPLIT_PATTERN.split(url.getMethodParameter(methodName, "hash.arguments", "0"));
            argumentIndex = new int[index.length];
            for (int i = 0; i < index.length; i++) {
                argumentIndex[i] = Integer.parseInt(index[i]);
            }
            for (Invoker<T> invoker : invokers) {
                String address = invoker.getUrl().getAddress();
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(address + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        // here not put the object reference into the map， only put the address
                        virtualInvokers.put(m, address.intern());
                    }
                }
            }
        }

        public Invoker<T> select(Invocation invocation, Map<String, Invoker<T>> invokerMap) {
            Object[] param = RpcUtils.getArguments(invocation);
            String key = toKey(param);
            byte[] digest = md5(key);
            String invokerAddress = selectForKey(hash(digest, 0));
            return selectInvoker(invokerAddress, invokerMap);
        }

        private String toKey(Object[] args) {
            StringBuilder buf = new StringBuilder();
            for (int i : argumentIndex) {
                if (i >= 0 && i < args.length) {
                    // find a problem here, when args[i] is an array, 
                    // this method is not stable. Because the value of args[i]
                    // may be the memory address.
                    buf.append(args[i]);
                }
            }
            return buf.toString();
        }

        private String selectForKey(long hash) {
            Long key = hash;
            if (!virtualInvokers.containsKey(key)) {
                SortedMap<Long, String> tailMap = virtualInvokers.tailMap(key);
                if (tailMap.isEmpty()) {
                    key = virtualInvokers.firstKey();
                } else {
                    key = tailMap.firstKey();
                }
            }
            return virtualInvokers.get(key);
        }

        private Invoker<T> selectInvoker(String invokerAddress, Map<String, Invoker<T>> invokerMap) {
            return invokerMap.get(invokerAddress);
        }

        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
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
            byte[] bytes;
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
