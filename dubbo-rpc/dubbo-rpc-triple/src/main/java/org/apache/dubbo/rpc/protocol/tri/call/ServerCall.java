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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.rpc.TriRpcStatus;

import java.util.Map;

/**
 * ServerCall manipulates server details of a RPC call. Request messages are acquired by {@link
 * Listener}. Backpressure is supported by {@link #request(int)}.Response messages are sent by
 * {@link ServerCall#sendMessage(Object)}.
 */
public interface ServerCall {

    interface Listener {

        void onMessage(Object message);

        void onCancel(TriRpcStatus status);

        void onComplete();
    }

    void sendMessage(Object message);

    void request(int numMessages);

    void close(TriRpcStatus status, Map<String, Object> responseAttrs);

}
