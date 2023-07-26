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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RpcServerContextAttachment extends RpcContextAttachment {
    @Override
    public RpcContextAttachment copyOf(boolean needCopy) {
        throw new RuntimeException("copyOf internal method, should not be invoke");
    }

    @Override
    protected boolean isValid() {
        throw new RuntimeException("isValid of is internal method, should not be invoke");
    }

    @Override
    public RpcContextAttachment setObjectAttachment(String key, Object value) {
        RpcContext.getServerResponseContext().setObjectAttachment(key, value);
        return this;
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
        return RpcContext.getServerResponseContext().getAsyncContext();
    }

    @Override
    public String getAttachment(String key) {
        Object attachment = getObjectAttachment(key);
        if (attachment instanceof String) {
            return (String) attachment;
        }
        return null;
    }

    @Override
    public Object getObjectAttachment(String key) {
        Object fromServerResponse = RpcContext.getServerResponseContext().getObjectAttachment(key);
        if (fromServerResponse == null) {
            fromServerResponse = RpcContext.getClientResponseContext().getObjectAttachment(key);
        }
        return fromServerResponse;
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
        RpcContext.getServerResponseContext().removeAttachment(key);
        RpcContext.getClientResponseContext().removeAttachment(key);
        return this;
    }

    @Override
    public Map<String, String> getAttachments() {
        return new AttachmentsAdapter.ObjectToStringMap(new ObjectAttachmentMap(this));
    }

    @Override
    public Map<String, Object> getObjectAttachments() {
        return new ObjectAttachmentMap(this);
    }

    @Override
    public RpcContextAttachment setAttachments(Map<String, String> attachment) {
        RpcContext.getServerResponseContext().setAttachments(attachment);
        RpcContext.getClientResponseContext().clearAttachments();
        return this;
    }

    @Override
    public RpcContextAttachment setObjectAttachments(Map<String, Object> attachment) {
        RpcContext.getServerResponseContext().setObjectAttachments(attachment);
        RpcContext.getClientResponseContext().clearAttachments();
        return this;
    }

    @Override
    public void clearAttachments() {
        RpcContext.getServerResponseContext().clearAttachments();
        RpcContext.getClientResponseContext().clearAttachments();
    }

    @Override
    public Map<String, Object> get() {
        return getObjectAttachments();
    }

    @Override
    public RpcContextAttachment set(String key, Object value) {
        return setAttachment(key, value);
    }

    @Override
    public RpcContextAttachment remove(String key) {
        return removeAttachment(key);
    }

    @Override
    public Object get(String key) {
        return getAttachment(key);
    }

    static class ObjectAttachmentMap implements Map<String, Object> {
        private final RpcServerContextAttachment adapter;

        public ObjectAttachmentMap(RpcServerContextAttachment adapter) {
            this.adapter = adapter;
        }

        private Map<String, Object> getAttachments() {
            Map<String, Object> clientResponse = RpcContext.getClientResponseContext().getObjectAttachments();
            Map<String, Object> serverResponse = RpcContext.getServerResponseContext().getObjectAttachments();
            Map<String, Object> result = new HashMap<>((int) (clientResponse.size() + serverResponse.size() / 0.75) + 1);
            result.putAll(clientResponse);
            result.putAll(serverResponse);
            return result;
        }

        @Override
        public int size() {
            return getAttachments().size();
        }

        @Override
        public boolean isEmpty() {
            return RpcContext.getClientResponseContext().getObjectAttachments().isEmpty() &&
                RpcContext.getServerResponseContext().getObjectAttachments().isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return RpcContext.getClientResponseContext().getObjectAttachments().containsKey(key) ||
                RpcContext.getServerResponseContext().getObjectAttachments().containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return RpcContext.getClientResponseContext().getObjectAttachments().containsValue(value) ||
                RpcContext.getServerResponseContext().getObjectAttachments().containsValue(value);
        }

        @Override
        public Object get(Object key) {
            if (key instanceof String) {
                return adapter.getObjectAttachment((String) key);
            } else {
                return null;
            }
        }

        @Override
        public Object put(String key, Object value) {
            return adapter.setObjectAttachment(key, value);
        }

        @Override
        public Object remove(Object key) {
            if (key instanceof String) {
                return adapter.removeAttachment((String) key);
            } else {
                return null;
            }
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            for (Entry<? extends String, ?> entry : m.entrySet()) {
                adapter.setObjectAttachment(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void clear() {
            adapter.clearAttachments();
        }

        @Override
        public Set<String> keySet() {
            return getAttachments().keySet();
        }

        @Override
        public Collection<Object> values() {
            return getAttachments().values();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return getAttachments().entrySet();
        }
    }
}
