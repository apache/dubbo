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

import org.apache.dubbo.common.stream.ServerCallStreamObserver;

import java.util.concurrent.atomic.AtomicInteger;

import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

public class CreateObserverAdapter {

    private ServerCallStreamObserver<String> responseObserver;
    private AtomicInteger nextCounter;
    private AtomicInteger completeCounter;
    private AtomicInteger errorCounter;

    CreateObserverAdapter() {

        nextCounter = new AtomicInteger();
        completeCounter = new AtomicInteger();
        errorCounter = new AtomicInteger();

        responseObserver = Mockito.mock(ServerCallStreamObserver.class);
        doAnswer(o -> nextCounter.incrementAndGet()).when(responseObserver).onNext(anyString());
        doAnswer(o -> completeCounter.incrementAndGet()).when(responseObserver).onCompleted();
        doAnswer(o -> errorCounter.incrementAndGet()).when(responseObserver).onError(any(Throwable.class));
    }

    public AtomicInteger getCompleteCounter() {
        return completeCounter;
    }

    public AtomicInteger getNextCounter() {
        return nextCounter;
    }

    public AtomicInteger getErrorCounter() {
        return errorCounter;
    }

    public ServerCallStreamObserver<String> getResponseObserver() {
        return this.responseObserver;
    }
}
