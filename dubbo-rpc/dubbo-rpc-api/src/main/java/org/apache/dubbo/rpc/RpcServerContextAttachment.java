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

public class RpcServerContextAttachment extends RpcContextAttachment{
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
        Object value = getObjectAttachment(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    @Override
    public Object getObjectAttachment(String key) {
        Object attachment = getServerResponseContext().getObjectAttachment(key);
        if (attachment != null) {
            return attachment;
        } else {
            return RpcContext.getClientResponseContext().getObjectAttachment(key);
        }
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
        RpcContext.getClientResponseContext().removeAttachment(key);
        return RpcContext.getServerResponseContext().removeAttachment(key);
    }

    @Override
    public Map<String, String> getAttachments() {
//        Map<String, String> attachmentMap = this;
//        Map<String, String> clientResponseContext = RpcContext.getClientResponseContext().getAttachments();
//        Map<String, String> serverResponseContext = RpcContext.getServerResponseContext().getAttachments();
//        attachmentMap.putAll(clientResponseContext);
//        attachmentMap.putAll(serverResponseContext);
        return new AttachmentMap(this);
//        return attachmentMap;
    }

    @Override
    public Map<String, Object> getObjectAttachments() {
        Map<String, Object> attachmentMap = new HashMap<>();
        Map<String, Object> clientResponseContext = RpcContext.getClientResponseContext().getObjectAttachments();
        Map<String, Object> serverResponseContext = RpcContext.getServerResponseContext().getObjectAttachments();
        attachmentMap.putAll(clientResponseContext);
        attachmentMap.putAll(serverResponseContext);
        return attachmentMap;
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
        RpcContext.getClientAttachment().clearAttachments();
        RpcContext.getServerResponseContext().clearAttachments();
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
    static class AttachmentMap implements Map{
        private RpcServerContextAttachment rpcServerContextAttachment;
        public AttachmentMap(RpcServerContextAttachment rpcServerContextAttachment) {
            this.rpcServerContextAttachment = rpcServerContextAttachment;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public Object put(Object key, Object value) {
            return rpcServerContextAttachment.setAttachment((String) key, value);
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map m) {
            System.out.println();
        }

        @Override
        public void clear() {

        }

        @Override
        public Set keySet() {
            return null;
        }

        @Override
        public Collection values() {
            return null;
        }

        @Override
        public Set<Entry> entrySet() {
            return null;
        }
    }


    /**
     * get server side context. ( A <-- B , in B side)
     *
     * @return server context
     */
    public static RpcContextAttachment getServerContext() {
        return new RpcServerContextAttachment();
    }
}
