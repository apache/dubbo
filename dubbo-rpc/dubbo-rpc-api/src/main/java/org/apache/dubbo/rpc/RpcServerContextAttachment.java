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
            return super.setObjectAttachment(key, value);
        }

        @Override
        protected void setAsyncContext(AsyncContext asyncContext) {
            super.setAsyncContext(asyncContext);
        }

        @Override
        public boolean isAsyncStarted() {
            return super.isAsyncStarted();
        }

        @Override
        public boolean stopAsync() {
            return super.stopAsync();
        }

        @Override
        public AsyncContext getAsyncContext() {
            return super.getAsyncContext();
        }

        @Override
        public String getAttachment(String key) {
            return super.getAttachment(key);
        }

        @Override
        public Object getObjectAttachment(String key) {
            return super.getObjectAttachment(key);
        }

        @Override
        public RpcContextAttachment setAttachment(String key, String value) {
            return super.setAttachment(key, value);
        }

        @Override
        public RpcContextAttachment setAttachment(String key, Object value) {
            return super.setAttachment(key, value);
        }

        @Override
        public RpcContextAttachment removeAttachment(String key) {
            return super.removeAttachment(key);
        }

        @Override
        public Map<String, String> getAttachments() {
            return super.getAttachments();
        }

        @Override
        public Map<String, Object> getObjectAttachments() {
            return super.getObjectAttachments();
        }

        @Override
        public RpcContextAttachment setAttachments(Map<String, String> attachment) {
            return super.setAttachments(attachment);
        }

        @Override
        public RpcContextAttachment setObjectAttachments(Map<String, Object> attachment) {
            return super.setObjectAttachments(attachment);
        }

        @Override
        public void clearAttachments() {
            super.clearAttachments();
        }

        @Override
        public Map<String, Object> get() {
            return super.get();
        }

        @Override
        public RpcContextAttachment set(String key, Object value) {
            return super.set(key, value);
        }

        @Override
        public RpcContextAttachment remove(String key) {
            return super.remove(key);
        }

        @Override
        public Object get(String key) {
            return super.get(key);
        }

        @Override
        public RpcContextAttachment copyOf(boolean needCopy) {
            return super.copyOf(needCopy);
        }

        @Override
        protected boolean isValid() {
            return super.isValid();
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
