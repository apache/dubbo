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

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public abstract class AbstractResult implements Result {
    protected Map<String, String> attachments = new HashMap<String, String>();

    protected Object result;

    protected Throwable exception;

    @Override
    public Map<String, String> getAttachments() {
        return attachments;
    }

    @Override
    public void setAttachments(Map<String, String> map) {
        this.attachments = map == null ? new HashMap<String, String>() : map;
    }

    @Override
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

    @Override
    public void setAttachment(String key, String value) {
        attachments.put(key, value);
    }

}
