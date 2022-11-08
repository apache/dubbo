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
import java.util.Objects;

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
        return (RpcContextAttachment) getObjectAttachments().put(key, value);
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
        return (String) getObjectAttachment(key);
    }

    @Override
    public Object getObjectAttachment(String key) {
        return getAttachmentObjectMap().get(key);
    }

    @Override
    public RpcContextAttachment setAttachment(String key, String value) {
        return (RpcContextAttachment) getAttachmentObjectMap().put(key, value);
//        return RpcContext.getServerResponseContext().setAttachment(key, value);
    }

    @Override
    public RpcContextAttachment setAttachment(String key, Object value) {
        return (RpcContextAttachment) getObjectAttachments().put(key, value);
    }

    @Override
    public RpcContextAttachment removeAttachment(String key) {
        return (RpcContextAttachment) getAttachmentObjectMap().remove(key);
    }

    @Override
    public Map<String, String> getAttachments() {
        return getAttachmentMap();
    }

    @Override
    public Map<String, Object> getObjectAttachments() {
        return getAttachmentObjectMap();
    }

    @Override
    public RpcContextAttachment setAttachments(Map<String, String> attachment) {
        getAttachmentMap().putAll(attachment);
        return (RpcContextAttachment) getAttachmentMap();
    }

    @Override
    public RpcContextAttachment setObjectAttachments(Map<String, Object> attachment) {
        getAttachmentObjectMap().putAll(attachment);
        return (RpcContextAttachment) getAttachmentObjectMap();
    }

    @Override
    public void clearAttachments() {
        getAttachmentMap().clear();
    }

    public Map<String, String> getAttachmentMap() {
        return new AttachmentMap(this);
    }

    public Map<String, Object> getAttachmentObjectMap() {
        return new AttachmentMap(this);
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

    static class AttachmentMap implements Map {
        private RpcServerContextAttachment rpcServerContextAttachment;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RpcServerContextAttachment.AttachmentMap that = (RpcServerContextAttachment.AttachmentMap) o;
            return Objects.equals(rpcServerContextAttachment, that.rpcServerContextAttachment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rpcServerContextAttachment);
        }

        public AttachmentMap(RpcServerContextAttachment rpcServerContextAttachment) {
            this.rpcServerContextAttachment = rpcServerContextAttachment;
        }

        public Map<String, String> getAllAttachmentMap() {
            Map<String, String> map = new HashMap<>();
            Map<String, String> clientResponseContext = RpcContext.getClientResponseContext().getAttachments();
            Map<String, String> serverResponseContext = RpcContext.getServerResponseContext().getAttachments();
            map.putAll(clientResponseContext);
            map.putAll(serverResponseContext);
            return map;
        }
        @Override
        public int size() {
            return getAllAttachmentMap().size();
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean containsKey(Object key) {
            return getAllAttachmentMap().containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return getAllAttachmentMap().containsValue(value);
        }

        @Override
        public Object get(Object key) {
            Object attachment = getServerResponseContext().getObjectAttachment((String) key);
            if (attachment != null) {
                return attachment;
            } else {
                return RpcContext.getClientResponseContext().getObjectAttachment((String) key);
            }
        }

        @Override
        public Object put(Object key, Object value) {
            return RpcContext.getServerResponseContext().setAttachment((String) key, value);
        }

        @Override
        public Object remove(Object key) {
            RpcContext.getClientResponseContext().removeAttachment((String) key);
            return RpcContext.getServerResponseContext().removeAttachment((String) key);
        }

        @Override
        public void putAll(Map m) {
            getAllAttachmentMap().putAll(m);
        }

        @Override
        public void clear() {
            getAllAttachmentMap().clear();
        }

        @Override
        public Set keySet() {
            return getAllAttachmentMap().keySet();
        }

        @Override
        public Collection values() {
            return getAllAttachmentMap().values();
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            return getAllAttachmentMap().entrySet();
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
