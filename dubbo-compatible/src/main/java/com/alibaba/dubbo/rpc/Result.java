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

package com.alibaba.dubbo.rpc;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Deprecated
public interface Result extends org.apache.dubbo.rpc.Result {

    @Override
    default void setValue(Object value) {

    }

    @Override
    default void setException(Throwable t) {

    }

    @Override
    default Map<String, Object> getObjectAttachments() {
        return Collections.emptyMap();
    }

    @Override
    default void addObjectAttachments(Map<String, Object> map) {

    }

    @Override
    default void setObjectAttachments(Map<String, Object> map) {

    }

    @Override
    default Object getObjectAttachment(String key) {
        return null;
    }

    @Override
    default Object getObjectAttachment(String key, Object defaultValue) {
        return null;
    }

    abstract class AbstractResult implements Result {

        @Override
        public void setValue(Object value) {

        }

        @Override
        public org.apache.dubbo.rpc.Result whenCompleteWithContext(BiConsumer<org.apache.dubbo.rpc.Result, Throwable> fn) {
            return null;
        }

        @Override
        public <U> CompletableFuture<U> thenApply(Function<org.apache.dubbo.rpc.Result, ? extends U> fn) {
            return null;
        }

        @Override
        public org.apache.dubbo.rpc.Result get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public org.apache.dubbo.rpc.Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }

    class CompatibleResult extends AbstractResult {
        private org.apache.dubbo.rpc.Result delegate;

        public CompatibleResult(org.apache.dubbo.rpc.Result result) {
            this.delegate = result;
        }

        public org.apache.dubbo.rpc.Result getDelegate() {
            return delegate;
        }

        @Override
        public Object getValue() {
            return delegate.getValue();
        }

        @Override
        public void setValue(Object value) {
            delegate.setValue(value);
        }

        @Override
        public Throwable getException() {
            return delegate.getException();
        }

        @Override
        public void setException(Throwable t) {
            delegate.setException(t);
        }

        @Override
        public boolean hasException() {
            return delegate.hasException();
        }

        @Override
        public Object recreate() throws Throwable {
            return delegate.recreate();
        }

        @Override
        public Map<String, String> getAttachments() {
            return delegate.getAttachments();
        }

        @Override
        public void addAttachments(Map<String, String> map) {
            delegate.addAttachments(map);
        }

        @Override
        public void setAttachments(Map<String, String> map) {
            delegate.setAttachments(map);
        }

        @Override
        public String getAttachment(String key) {
            return delegate.getAttachment(key);
        }

        @Override
        public String getAttachment(String key, String defaultValue) {
            return delegate.getAttachment(key, defaultValue);
        }

        @Override
        public void setAttachment(String key, String value) {
            delegate.setAttachment(key, value);
        }

        @Override
        public void setAttachment(String key, Object value) {
            delegate.setAttachment(key, value);
        }

        @Override
        public void setObjectAttachment(String key, Object value) {
            delegate.setObjectAttachment(key, value);
        }
    }
}
