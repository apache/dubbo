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
 * This class provides map adapters to support attachments in RpcContext, Invocation and Result switch from
 * <String, String> to <String, Object>
 */
public class AttachmentsAdapter {

    public static class ObjectToStringMap extends HashMap<String, String> {
        private final Map<String, Object> attachments;

        public ObjectToStringMap(Map<String, Object> attachments) {
            for (Entry<String, Object> entry : attachments.entrySet()) {
                String convertResult = convert(entry.getValue());
                if (convertResult != null) {
                    super.put(entry.getKey(), convertResult);
                }
            }
            this.attachments = attachments;
        }

        @Override
        public String put(String key, String value) {
            attachments.put(key, value);
            return super.put(key, value);
        }

        @Override
        public String remove(Object key) {
            attachments.remove(key);
            return super.remove(key);
        }

        private String convert(Object obj) {
            if (obj instanceof String) {
                return (String) obj;
            }
            return null; // or JSON.toString(obj);
        }

        @Override
        public void clear() {
            attachments.clear();
            super.clear();
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> map) {
            attachments.putAll(map);
            super.putAll(map);
        }
    }
}
