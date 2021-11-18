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

import java.util.concurrent.CountDownLatch;

/**
 * Usually use it to simulate a outboundMessageSubscriber
 */
public class MockStreamObserver implements StreamObserver<String> {
    private String onNextData;
    private Throwable onErrorThrowable;
    private boolean onCompleted;
    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void onNext(String data) {
        onNextData = data;
    }

    @Override
    public void onError(Throwable throwable) {
        onErrorThrowable = throwable;
    }

    @Override
    public void onCompleted() {
        onCompleted = true;
        latch.countDown();
    }

    public String getOnNextData() {
        return onNextData;
    }

    public Throwable getOnErrorThrowable() {
        return onErrorThrowable;
    }

    public boolean isOnCompleted() {
        return onCompleted;
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}
