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
package org.apache.dubbo.rpc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * It's recommended to implement Filter.Listener directly for callback registration, check the default implementation,
 * see {@link org.apache.dubbo.rpc.filter.ExceptionFilter}, for example.
 * <p>
 * If you do not want to share Listener instance between RPC calls. ListenableFilter can be used
 * to keep a 'one Listener each RPC call' model.
 */
@Deprecated
public abstract class ListenableFilter implements Filter {

    protected Listener listener = null;
    protected final ConcurrentMap<Invocation, Listener> listeners = new ConcurrentHashMap<>();

    public Listener listener() {
        return listener;
    }

    public Listener listener(Invocation invocation) {
        Listener invListener = listeners.get(invocation);
        if (invListener == null) {
            invListener = listener;
        }
        return invListener;
    }

    public void addListener(Invocation invocation, Listener listener) {
        listeners.putIfAbsent(invocation, listener);
    }

    public void removeListener(Invocation invocation) {
        listeners.remove(invocation);
    }
}
