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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.ServerStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.call.AbstractServerCall;

import java.util.Map;

public class ServerCallToObserverAdapter<T> extends CancelableStreamObserver<T> implements
    ServerStreamObserver<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelableStreamObserver.class);
    public final CancellationContext cancellationContext;
    private final AbstractServerCall call;
    private Map<String, Object> attachments;
    private boolean terminated = false;

    private boolean isNeedReturnException = false;

    private Integer exceptionCode = CommonConstants.TRI_EXCEPTION_CODE_NOT_EXISTS;

    public Integer getExceptionCode() {
        return exceptionCode;
    }

    public void setExceptionCode(Integer exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    public boolean isNeedReturnException() {
        return isNeedReturnException;
    }

    public void setNeedReturnException(boolean needReturnException) {
        isNeedReturnException = needReturnException;
    }

    public ServerCallToObserverAdapter(AbstractServerCall call,
        CancellationContext cancellationContext) {
        this.call = call;
        this.cancellationContext = cancellationContext;
    }

    public boolean isAutoRequestN() {
        return call.isAutoRequestN();
    }


    public boolean isTerminated() {
        return terminated;
    }

    private void setTerminated() {
        this.terminated = true;
    }

    @Override
    public void onNext(Object data) {
        if (isTerminated()) {
            throw new IllegalStateException(
                "Stream observer has been terminated, no more data is allowed");
        }
        call.setExceptionCode(exceptionCode);
        call.setNeedReturnException(isNeedReturnException);
        call.sendMessage(data);
    }

    @Override
    public void onError(Throwable throwable) {
        final TriRpcStatus status = TriRpcStatus.getStatus(throwable);
        onCompleted(status);
    }

    public void onCompleted(TriRpcStatus status) {
        if (isTerminated()) {
            return;
        }
        call.setExceptionCode(exceptionCode);
        call.setNeedReturnException(isNeedReturnException);
        call.close(status, attachments);
        setTerminated();
    }

    @Override
    public void onCompleted() {
        onCompleted(TriRpcStatus.OK);
    }

    public void setResponseAttachments(Map<String, Object> attachments) {
        this.attachments = attachments;
    }

    @Override
    public void setCompression(String compression) {
        call.setCompression(compression);
    }

    public void cancel(Throwable throwable) {
        if (terminated) {
            return;
        }
        setTerminated();
        call.cancelByLocal(throwable);
    }

    public boolean isTimeout(long cost) {
        return call.timeout != null && call.timeout < cost;
    }

    @Override
    public void disableAutoFlowControl() {
        call.disableAutoRequestN();
    }

    @Override
    public void request(int count) {
        call.request(count);
    }
}
