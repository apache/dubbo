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

import org.apache.dubbo.common.Experimental;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

public class RpcContextAttachment extends RpcContext{
    protected volatile Map<String, Object> attachments = new HashMap<>();

    protected RpcContextAttachment() {
    }

    /**
     * also see {@link #getObjectAttachment(String)}.
     *
     * @param key
     * @return attachment
     */
    @Override
    public String getAttachment(String key) {
        Object value = attachments.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null; // or JSON.toString(value);
    }

    /**
     * get attachment.
     *
     * @param key
     * @return attachment
     */
    @Override
    @Experimental("Experiment api for supporting Object transmission")
    public Object getObjectAttachment(String key) {
        return attachments.get(key);
    }

    /**
     * set attachment.
     *
     * @param key
     * @param value
     * @return context
     */
    @Override
    public RpcContextAttachment setAttachment(String key, String value) {
        return setObjectAttachment(key, (Object) value);
    }

    @Override
    public RpcContextAttachment setAttachment(String key, Object value) {
        return setObjectAttachment(key, value);
    }

    @Override
    @Experimental("Experiment api for supporting Object transmission")
    public RpcContextAttachment setObjectAttachment(String key, Object value) {
        if (value == null) {
            attachments.remove(key);
        } else {
            attachments.put(key, value);
        }
        return this;
    }

    /**
     * remove attachment.
     *
     * @param key
     * @return context
     */
    @Override
    public RpcContextAttachment removeAttachment(String key) {
        attachments.remove(key);
        return this;
    }

    /**
     * get attachments.
     *
     * @return attachments
     */
    @Override
    @Deprecated
    public Map<String, String> getAttachments() {
        return new AttachmentsAdapter.ObjectToStringMap(this.getObjectAttachments());
    }

    /**
     * get attachments.
     *
     * @return attachments
     */
    @Override
    @Experimental("Experiment api for supporting Object transmission")
    public Map<String, Object> getObjectAttachments() {
        return attachments;
    }

    /**
     * set attachments
     *
     * @param attachment
     * @return context
     */
    @Override
    public RpcContextAttachment setAttachments(Map<String, String> attachment) {
        this.attachments.clear();
        if (attachment != null && attachment.size() > 0) {
            this.attachments.putAll(attachment);
        }
        return this;
    }

    /**
     * set attachments
     *
     * @param attachment
     * @return context
     */
    @Override
    @Experimental("Experiment api for supporting Object transmission")
    public RpcContextAttachment setObjectAttachments(Map<String, Object> attachment) {
        this.attachments.clear();
        if (CollectionUtils.isNotEmptyMap(attachment)) {
            this.attachments = attachment;
        }
        return this;
    }

    @Override
    public void clearAttachments() {
        this.attachments.clear();
    }

    /**
     * get values.
     *
     * @return values
     */
    @Override
    @Deprecated
    public Map<String, Object> get() {
        return getObjectAttachments();
    }

    /**
     * set value.
     *
     * @param key
     * @param value
     * @return context
     */
    @Override
    @Deprecated
    public RpcContextAttachment set(String key, Object value) {
        return setAttachment(key, value);
    }

    /**
     * remove value.
     *
     * @param key
     * @return value
     */
    @Override
    @Deprecated
    public RpcContextAttachment remove(String key) {
        return removeAttachment(key);
    }

    /**
     * get value.
     *
     * @param key
     * @return value
     */
    @Override
    @Deprecated
    public Object get(String key) {
        return getAttachment(key);
    }

    /**
     * Also see {@link RpcServiceContext#copyOf(boolean)}
     *
     * @return a copy of RpcContextAttachment with deep copied attachments
     */
    public RpcContextAttachment copyOf(boolean needCopy) {
        if (!isValid()) {
            return null;
        }

        if (needCopy) {
            RpcContextAttachment copy = new RpcContextAttachment();
            if (CollectionUtils.isNotEmptyMap(attachments)) {
                copy.attachments.putAll(this.attachments);
            }
            return copy;
        } else {
            return this;
        }
    }

    private boolean isValid() {
        return CollectionUtils.isNotEmptyMap(attachments);
    }
}
