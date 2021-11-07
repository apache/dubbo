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

import org.apache.dubbo.rpc.Invoker;

import java.util.Collections;
import java.util.Map;

/***
 * Cache the address list for each Router.
 * @param <T>
 * @since 3.0
 */
public class RouterCache<T> {
    private final Map<String, BitList<Invoker<T>>> addressPool;
    private final Object routerMetadata;

    public RouterCache() {
        this(Collections.emptyMap(), null);
    }

    public RouterCache(Map<String, BitList<Invoker<T>>> addressPool) {
        this(addressPool, null);
    }

    public RouterCache(Map<String, BitList<Invoker<T>>> addressPool, Object routerMetadata) {
        this.addressPool = addressPool;
        this.routerMetadata = routerMetadata;
    }

    public Map<String, BitList<Invoker<T>>> getAddressPool() {
        return addressPool;
    }

    public Object getRouterMetadata() {
        return routerMetadata;
    }
}
