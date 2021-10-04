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
package org.apache.dubbo.rpc.cluster.router.state;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.dubbo.rpc.Invoker;

/***
 * Cache the address list for each Router.
 * @param <T>
 * @since 3.0
 */
public class RouterCache<T> {
    private final static ConcurrentHashMap EMPTY_MAP = new ConcurrentHashMap<>();
    protected ConcurrentMap<String, BitList<Invoker<T>>> addrPool = EMPTY_MAP;
    protected Object addrMetadata;

    public ConcurrentMap<String, BitList<Invoker<T>>> getAddrPool() {
        return addrPool;
    }

    public void setAddrPool(ConcurrentHashMap<String, BitList<Invoker<T>>> addrPool) {
        this.addrPool = addrPool;
    }

    public Object getAddrMetadata() {
        return addrMetadata;
    }

    public void setAddrMetadata(Object addrMetadata) {
        this.addrMetadata = addrMetadata;
    }
}
