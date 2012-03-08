/*
 * Copyright 1999-2011 Alibaba Group.
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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.utils.AtomicPositiveInteger;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;

/**
 * Round robin load balance.
 *
 * @author qian.lei
 * @author william.liangf
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "roundrobin"; 
    
    private final ConcurrentMap<String, RoundRobinSequence> sequences = new ConcurrentHashMap<String, RoundRobinSequence>();

    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, Invocation invocation) {
        String key = invokers.get(0).getInterface().getName() + "." + invocation.getMethodName();// + System.identityHashCode(invokers);
        RoundRobinSequence sequence = sequences.get(key);
        int identityHashCode = System.identityHashCode(invokers);
        if (sequence == null || sequence.getIdentityHashCode() != identityHashCode) {
            sequences.put(key, new RoundRobinSequence(identityHashCode));
            sequence = sequences.get(key);
        }
        // 取模轮循
        return invokers.get(sequence.getAndIncrement() % invokers.size());
    }
    
    private static final class RoundRobinSequence {
        
        private final AtomicPositiveInteger i = new AtomicPositiveInteger();

        private final int                       identityHashCode;

        public RoundRobinSequence(int identityHashCode) {
            this.identityHashCode = identityHashCode;
        }

        public int getAndIncrement() {
            return i.getAndIncrement();
        }

        public int getIdentityHashCode() {
            return identityHashCode;
        }
        
    }

}