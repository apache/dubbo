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
package org.apache.dubbo.rpc.protocol.tri.observer;

import org.apache.dubbo.common.stream.ClientCallStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.ClientStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.call.ClientCall;

public class ClientCallToObserverAdapter<T> extends CancelableStreamObserver<T>
        implements ClientStreamObserver<T>, ClientCallStreamObserver<T> {

    private final ClientCall call;
    private final boolean streamingResponse;
    private boolean terminated;
    private Runnable onReadyHandler;

    public ClientCallToObserverAdapter(ClientCall call, boolean streamingResponse) {
        this.call = call;
        this.streamingResponse = streamingResponse;
    }

    public boolean isAutoRequestEnabled() {
        return call.isAutoRequest();
    }

    @Override
    public void onNext(Object data) {
        if (terminated) {
            throw new IllegalStateException("Stream observer has been terminated, no more data is allowed");
        }
        call.sendMessage(data);
    }

    @Override
    public void onError(Throwable throwable) {
        call.cancelByLocal(throwable);
        this.terminated = true;
    }

    @Override
    public void onCompleted() {
        if (terminated) {
            return;
        }
        call.halfClose();
        this.terminated = true;
    }

    @Override
    public void cancel(Throwable throwable) {
        call.cancelByLocal(throwable);
        this.terminated = true;
    }

    @Override
    public void setCompression(String compression) {
        call.setCompression(compression);
    }

    @Override
    public boolean isReady() {
        return call.isReady();
    }

    @Override
    public void setOnReadyHandler(Runnable onReadyHandler) {
        // Store locally, to be triggered by ObserverToClientCallListenerAdapter.onReady()
        this.onReadyHandler = onReadyHandler;
    }

    /**
     * Get the onReadyHandler for use by ClientCall.Listener.onReady().
     */
    public Runnable getOnReadyHandler() {
        return onReadyHandler;
    }

    @Override
    public void request(int count) {
        call.request(count);
    }

    @Override
    public void disableAutoFlowControl() {
        call.setAutoRequest(false);
    }

    @Override
    public void disableAutoRequestWithInitial(int request) {
        call.setAutoRequestWithInitial(request);
    }
}
