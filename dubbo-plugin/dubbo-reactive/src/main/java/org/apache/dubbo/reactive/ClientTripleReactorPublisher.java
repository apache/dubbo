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
package org.apache.dubbo.reactive;

import org.apache.dubbo.common.stream.CallStreamObserver;
import org.apache.dubbo.common.stream.ClientCallStreamObserver;
import org.apache.dubbo.common.stream.ClientResponseObserver;

import java.util.function.Consumer;

/**
 * Used in OneToMany & ManyToOne & ManyToMany in client. <br>
 * It is a Publisher for user subscriber to subscribe. <br>
 * It is a StreamObserver for responseStream. <br>
 * It is a Subscription for user subscriber to request and pass request to requestStream.
 * <p>
 * Implements {@link ClientResponseObserver} following gRPC's pattern where
 * {@link #beforeStart(ClientCallStreamObserver)} is called before the stream starts,
 * allowing configuration of flow control before any messages are sent.
 */
public class ClientTripleReactorPublisher<T> extends AbstractTripleReactorPublisher<T>
        implements ClientResponseObserver<Object, T> {

    public ClientTripleReactorPublisher() {}

    public ClientTripleReactorPublisher(Consumer<CallStreamObserver<?>> onSubscribe, Runnable shutdownHook) {
        super(onSubscribe, shutdownHook);
    }

    /**
     * Called by the runtime prior to the start of a call to provide a reference to the
     * {@link ClientCallStreamObserver} for the outbound stream.
     * <p>
     * Following gRPC's pattern, this method is called BEFORE {@code call.start()},
     * allowing configuration of onReadyHandler and flow control settings.
     */
    @Override
    public void beforeStart(ClientCallStreamObserver<Object> requestStream) {
        super.onSubscribe(requestStream);
    }
}
