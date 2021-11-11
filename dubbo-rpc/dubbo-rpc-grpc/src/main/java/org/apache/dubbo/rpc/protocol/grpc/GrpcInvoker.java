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
package org.apache.dubbo.rpc.protocol.grpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;

import io.grpc.Status;
import io.grpc.StatusException;

import java.util.concurrent.locks.ReentrantLock;

public class GrpcInvoker<T> extends AbstractInvoker<T> {
    private final ReentrantLock destroyLock = new ReentrantLock();

    private final Invoker<T> target;
    private ReferenceCountManagedChannel channel;

//    private static List<Exception> grpcExceptions = new ArrayList<>();
//    static {
//        grpcExceptions.add();
//    }

    public GrpcInvoker(Class<T> type, URL url, Invoker<T> target, ReferenceCountManagedChannel channel) {
        super(type, url);
        this.target = target;
        this.channel = channel;
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws Throwable {
        try {
            Result result = target.invoke(invocation);
            // FIXME result is an AsyncRpcResult instance.
            Throwable e = result.getException();
            if (e != null) {
                throw getRpcException(getInterface(), getUrl(), invocation, e);
            }
            return result;
        } catch (RpcException e) {
            if (e.getCode() == RpcException.UNKNOWN_EXCEPTION) {
                e.setCode(getErrorCode(e.getCause()));
            }
            throw e;
        } catch (Throwable e) {
            throw getRpcException(getInterface(), getUrl(), invocation, e);
        }
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable() && !channel.isShutdown() && !channel.isTerminated();
    }

    @Override
    public boolean isDestroyed() {
        return super.isDestroyed() || channel.isShutdown() || channel.isTerminated();
    }

    @Override
    public void destroy() {
        if (!super.isDestroyed()) {
            // double check to avoid dup close
            destroyLock.lock();
            try {
                if (super.isDestroyed()) {
                    return;
                }
                super.destroy();
                channel.shutdown();
            } finally {
                destroyLock.unlock();
            }
        }
    }

    private RpcException getRpcException(Class<?> type, URL url, Invocation invocation, Throwable e) {
        RpcException re = new RpcException("Failed to invoke remote service: " + type + ", method: "
                + invocation.getMethodName() + ", cause: " + e.getMessage(), e);
        re.setCode(getErrorCode(e));
        return re;
    }

    /**
     * FIXME, convert gRPC exceptions to equivalent Dubbo exceptions.
     *
     * @param e
     * @return
     */
    private int getErrorCode(Throwable e) {
        if (e instanceof StatusException) {
            StatusException statusException = (StatusException) e;
            Status status = statusException.getStatus();
            if (status.getCode() == Status.Code.DEADLINE_EXCEEDED) {
                return RpcException.TIMEOUT_EXCEPTION;
            }
        }
        return RpcException.UNKNOWN_EXCEPTION;
    }
}
