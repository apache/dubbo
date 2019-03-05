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
package org.apache.dubbo.rpc;

import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

/**
 * RPC Result.
 *
 * @serial Don't change the class name and properties.
 */
public class RpcResult extends AbstractResult {

    private static final long serialVersionUID = -6925924956850004727L;

    public RpcResult() {
    }

    public RpcResult(Object result) {
        this.setValue(result);
    }

    public RpcResult(Throwable exception) {
        this.setException(exception);
    }

    @Override
    public Object recreate() throws Throwable {
        if (RpcUtils.isReturnTypeFuture()) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            this.whenComplete((obj, t) -> {
                if (t != null) {
                    if (t instanceof CompletionException) {
                        t = t.getCause();
                    }
                    future.completeExceptionally(t);
                } else {
                    future.complete(obj);
                }
            });
            return future;
        } else {
            if (exception != null) {
                throw handleEmptyStacktrace(exception);
            }
            return result;
        }
    }

    private Throwable handleEmptyStacktrace (Throwable t) {
        // fix issue#619
        try {
            // get Throwable class
            Class clazz = exception.getClass();
            while (!clazz.getName().equals(Throwable.class.getName())) {
                clazz = clazz.getSuperclass();
            }
            // get stackTrace value
            Field stackTraceField = clazz.getDeclaredField("stackTrace");
            stackTraceField.setAccessible(true);
            Object stackTrace = stackTraceField.get(exception);
            if (stackTrace == null) {
                exception.setStackTrace(new StackTraceElement[0]);
            }
        } catch (Exception e) {
            // ignore
        }
        return  exception;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        return super.get();
    }

    @Override
    public Object getValue() {
        return this.get();
    }

    public void setValue(Object value) {
        this.result = value;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable e) {
        this.exception = e;
    }

    @Override
    public boolean hasException() {
        return exception != null;
    }

    @Override
    public String toString() {
        return "RpcResult [result=" + result + ", exception=" + exception + "]";
    }

    public final BiFunction<Object, Throwable, Response.AppResult> rpcResultToAppResult = (obj, t) -> {
        Response.AppResult appResult = new Response.AppResult();
        if (t != null) {
            appResult.setException(t);
        } else {
            if (this.hasException()) {
                appResult.setException(this.getException());
            } else {
                appResult.setResult(obj);
            }
        }
        appResult.setAttachments(this.getAttachments());
        return appResult;
    };

    public final BiFunction<Object, Throwable, Result> appResultToRpcResult = (obj, t) -> {
        if (t != null) {
            this.setException(t);

        } else {
            Response.AppResult appResult = (Response.AppResult) obj;
            if (appResult.getException() != null) {
                this.setException(appResult.getException());
            } else {
                this.setValue(appResult.getResult());
            }
            this.setAttachments(appResult.getAttachments());
        }
        return this;
    };


}
