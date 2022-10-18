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
import java.util.stream.Collectors;

public class AttachmentInSensitiveAppResponse extends AppResponse {

    @Override
    public void setAttachments(Map<String, String> map) {
        attachments = safeGetAttachments(map)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(it -> it.getKey().toLowerCase(), Map.Entry::getValue));
    }

    @Override
    public void setObjectAttachments(Map<String, Object> map) {
        attachments = safeGetAttachments(map)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(it -> it.getKey().toLowerCase(), Map.Entry::getValue));
    }


    protected void addObjectAttachments(Map<String, ?> map, Map<String, Object> attachments) {
        if (map == null) {
            return;
        }
        if (attachments == null) {
            attachments = new HashMap<>(map.size());
        }
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            attachments.put(entry.getKey().toLowerCase(), entry.getValue());
        }
    }


    @Override
    public String getAttachment(String key) {
        if (key == null) {
            return null;
        }
        String lowerCaseKey = key.toLowerCase();
        return super.getAttachment(lowerCaseKey);
    }

    @Override
    public Object getObjectAttachment(String key) {
        if (key == null) {
            return null;
        }
        String lowerCaseKey = key.toLowerCase();
        return super.getObjectAttachment(lowerCaseKey);
    }


    @Override
    public Object getObjectAttachment(String key, Object defaultValue) {
        if (key == null) {
            return null;
        }
        String lowerCaseKey = key.toLowerCase();
        return super.getObjectAttachment(lowerCaseKey, defaultValue);
    }

    @Override
    public void setAttachment(String key, String value) {
        if (key == null) {
            return;
        }
        String lowerCaseKey = key.toLowerCase();
        super.setAttachment(lowerCaseKey, value);
    }

    @Override
    public void setAttachment(String key, Object value) {
        if (key == null) {
            return;
        }
        String lowerCaseKey = key.toLowerCase();
        super.setAttachment(lowerCaseKey, value);
    }

    @Override
    public void setObjectAttachment(String key, Object value) {
        if (key == null) {
            return;
        }
        String lowerCaseKey = key.toLowerCase();
        super.setObjectAttachment(lowerCaseKey, value);
    }


}