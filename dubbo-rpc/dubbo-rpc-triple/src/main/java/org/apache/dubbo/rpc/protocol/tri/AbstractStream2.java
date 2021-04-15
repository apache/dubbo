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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.model.MethodDescriptor;

public abstract class AbstractStream2 implements Stream {
    private final URL url;
    private MethodDescriptor methodDescriptor;
    private StreamObserver<Object> streamSubscriber;
    private StreamObserver<Object> streamObserver;
    private TransportObserver transportObserver;
    private TransportObserver transportSubscriber;

    protected AbstractStream2(URL url) {
        this.url = url;
    }

    public StreamObserver<Object> getStreamSubscriber() {
        return streamSubscriber;
    }

    public StreamObserver<Object> getStreamObserver() {
        return streamObserver;
    }

    public TransportObserver getTransportSubscriber() {
        return transportSubscriber;
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public void setMethodDescriptor(MethodDescriptor methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public void subscribe(StreamObserver<Object> observer) {
        this.streamSubscriber = observer;
    }

    @Override
    public void subscribe(TransportObserver observer) {
        this.transportSubscriber = observer;
    }

    @Override
    public StreamObserver<Object> asStreamObserver() {
        return streamObserver;
    }

    @Override
    public TransportObserver asTransportObserver() {
        return transportObserver;
    }

}
