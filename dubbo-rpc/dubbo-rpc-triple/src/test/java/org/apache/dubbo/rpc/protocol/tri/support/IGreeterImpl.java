/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.dubbo.rpc.protocol.tri.support;

import org.apache.dubbo.common.stream.StreamObserver;

public class IGreeterImpl implements IGreeter {

    private StreamObserver<String> mockStreamObserver = new MockStreamObserver();

    @Override
    public String echo(String request) {
        return request;
    }

    @Override
    public void serverStream(String str, StreamObserver<String> observer) {
        System.out.println("srt=" + str);
        observer.onNext(str);
        observer.onCompleted();
    }

    @Override
    public StreamObserver<String> bidirectionalStream(StreamObserver<String> observer) {
        observer.onNext(SERVER_MSG);
        observer.onCompleted();
        return mockStreamObserver; // This will serve as the server's outboundMessageSubscriber
    }

    public StreamObserver<String> getMockStreamObserver() {
        return mockStreamObserver;
    }
}
