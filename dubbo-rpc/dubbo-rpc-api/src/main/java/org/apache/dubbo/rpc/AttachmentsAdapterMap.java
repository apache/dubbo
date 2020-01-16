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

public class AttachmentsAdapterMap extends HashMap<String, String> {
    private Map<String, Object> attachments;

    public AttachmentsAdapterMap(Map<String, Object> attachments) {
        this.attachments = attachments;
    }

    @Override
    public String get(Object key) {
        Object obj = attachments.get(key);
        return convert(obj);
    }

    @Override
    public String put(String key, String value) {
        Object obj = attachments.put(key, value);
        return convert(obj);
    }

    @Override
    public String remove(Object key) {
        Object obj = attachments.remove(key);
        return convert(obj);
    }

    private String convert(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        }
        return null; // or JSON.toString(obj);
    }
}
