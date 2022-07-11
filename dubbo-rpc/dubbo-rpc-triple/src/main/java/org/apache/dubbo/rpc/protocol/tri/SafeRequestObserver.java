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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.stream.StreamObserver;

/**
 * The StreamObserver for downstream of a stream to start request safely.
 */
public interface SafeRequestObserver<T> extends StreamObserver<T> {

    /**
     * In {@code startRequest}, {@link org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver#request(int)} can be used safely,
     * because the lower-level call has already started.
     */
    void startRequest();
}
