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

import java.util.Map;

@Deprecated
public interface Invocation extends org.apache.dubbo.rpc.Invocation {

    @Override
    Invoker<?> getInvoker();

    default org.apache.dubbo.rpc.Invocation getOriginal() {
        return null;
    }

    class CompatibleInvocation implements Invocation {

        private org.apache.dubbo.rpc.Invocation delegate;

        public CompatibleInvocation(org.apache.dubbo.rpc.Invocation invocation) {
            this.delegate = invocation;
        }

        @Override
        public String getMethodName() {
            return delegate.getMethodName();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return delegate.getParameterTypes();
        }

        @Override
        public Object[] getArguments() {
            return delegate.getArguments();
        }

        @Override
        public Map<String, String> getAttachments() {
            return delegate.getAttachments();
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
        public Invoker<?> getInvoker() {
            return new Invoker.CompatibleInvoker(delegate.getInvoker());
        }

        @Override
        public org.apache.dubbo.rpc.Invocation getOriginal() {
            return delegate;
        }
    }
}
