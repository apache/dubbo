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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * RPC Result.
 *
 * @serial Don't change the class name and properties.
 */
public class RpcResult implements Result, Serializable {

    private static final long serialVersionUID = -6925924956850004727L;

    private Object result;

    private Throwable exception;

    private Map<String, String> attachments = new HashMap<String, String>();

    public RpcResult() {
    }

    public RpcResult(Object result) {
        this.result = result;
    }

    public RpcResult(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public Object recreate() throws Throwable {
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    @Override
    public Object getValue() {
        return result;
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
    public Map<String, String> getAttachments() {
        return attachments;
    }

    /**
     * Append all items from the map into the attachment, if map is empty then nothing happens
     *
     * @param map contains all key-value pairs to append
     */
    public void setAttachments(Map<String, String> map) {
        this.attachments = map == null ? new HashMap<String, String>() : map;
    }

    public void addAttachments(Map<String, String> map) {
        if (map == null) {
            return;
        }
        if (this.attachments == null) {
            this.attachments = new HashMap<String, String>();
        }
        this.attachments.putAll(map);
    }

    @Override
    public String getAttachment(String key) {
        return attachments.get(key);
    }

    @Override
    public String getAttachment(String key, String defaultValue) {
        String result = attachments.get(key);
        if (result == null || result.length() == 0) {
            result = defaultValue;
        }
        return result;
    }

    public void setAttachment(String key, String value) {
        attachments.put(key, value);
    }

    @Override
    public Result thenApplyWithContext(Function<Result, Result> fn) {
        throw new UnsupportedOperationException("RpcResult represents an concrete response, there will be no status changes, you should get internal values directly.");
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<Result, ? extends U> fn) {
        throw new UnsupportedOperationException("RpcResult represents an concrete response, there will be no status changes, you should get internal values directly.");
    }

    @Override
    public Result get() throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException("RpcResult represents an concrete response, there will be no status changes, you should get internal values directly.");
    }

    @Override
    public String toString() {
        return "RpcResult [result=" + result + ", exception=" + exception + "]";
    }
}
