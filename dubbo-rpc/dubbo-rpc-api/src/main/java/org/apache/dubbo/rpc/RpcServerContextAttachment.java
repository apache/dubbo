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

import java.util.Map;

public class RpcServerContextAttachment extends RpcContextAttachment{
    private static final RpcContextAttachment AGENT_SERVER_CONTEXT = new RpcContextAttachment() {
        @Override
        public RpcContextAttachment setObjectAttachment(String key, Object value) {
            return RpcContext.getServerResponseContext().setObjectAttachment(key, value);
        }

        @Override
        protected void setAsyncContext(AsyncContext asyncContext) {
            RpcContext.getServerResponseContext().setAsyncContext(asyncContext);
        }

        @Override
        public boolean isAsyncStarted() {
            return RpcContext.getServerResponseContext().isAsyncStarted();
        }

        @Override
        public boolean stopAsync() {
            return RpcContext.getServerResponseContext().stopAsync();
        }

        @Override
        public AsyncContext getAsyncContext() {
            return RpcContext.getClientResponseContext().getAsyncContext();
        }

        @Override
        public String getAttachment(String key) {
            return RpcContext.getClientResponseContext().getAttachment(key);
        }

        @Override
        public Object getObjectAttachment(String key) {
            return RpcContext.getClientResponseContext().getObjectAttachment(key);
        }

        @Override
        public RpcContextAttachment setAttachment(String key, String value) {
            return RpcContext.getServerResponseContext().setAttachment(key, value);
        }

        @Override
        public RpcContextAttachment setAttachment(String key, Object value) {
            return RpcContext.getServerResponseContext().setAttachment(key, value);
        }

        @Override
        public RpcContextAttachment removeAttachment(String key) {
            return RpcContext.getServerResponseContext().removeAttachment(key);
        }

        @Override
        public Map<String, String> getAttachments() {
            return RpcContext.getServerResponseContext().getAttachments();
        }

        @Override
        public Map<String, Object> getObjectAttachments() {
            return RpcContext.getServerResponseContext().getObjectAttachments();
        }

        @Override
        public RpcContextAttachment setAttachments(Map<String, String> attachment) {
            return RpcContext.getServerResponseContext().setAttachments(attachment);
        }

        @Override
        public RpcContextAttachment setObjectAttachments(Map<String, Object> attachment) {
            return RpcContext.getServerResponseContext().setObjectAttachments(attachment);
        }

        @Override
        public void clearAttachments() {
            RpcContext.getServerResponseContext().clearAttachments();
        }

        @Override
        public Map<String, Object> get() {
            return RpcContext.getServerResponseContext().get();
        }

        @Override
        public RpcContextAttachment set(String key, Object value) {
            return RpcContext.getServerResponseContext().set(key, value);
        }

        @Override
        public RpcContextAttachment remove(String key) {
            return RpcContext.getServerResponseContext().remove(key);
        }

        @Override
        public Object get(String key) {
            return RpcContext.getServerResponseContext().get(key);
        }

        @Override
        public RpcContextAttachment copyOf(boolean needCopy) {
            return RpcContext.getServerResponseContext().copyOf(needCopy);
        }

        @Override
        protected boolean isValid() {
            return RpcContext.getServerResponseContext().isValid();
        }
    };

    /**
     * get server side context. ( A <-- B , in B side)
     *
     * @return server context
     */
    public static RpcContextAttachment getServerContext() {
        return AGENT_SERVER_CONTEXT;
    }
}
