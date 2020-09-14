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
package org.apache.dubbo.common.url.component;

import org.apache.dubbo.common.URLStrParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;

public class URLParam {
    public static URLParam parseEncoded(String rawParam) {
        Map<String, String> parameters = URLStrParser.parseEncodedParams(rawParam);
        return new URLParam(parameters);
    }

    public static URLParam parse(String rawParam) {
        String[] parts = rawParam.split("&");
        Map<String, String> parameters = new HashMap<>();
        for (String part : parts) {
            part = part.trim();
            if (part.length() > 0) {
                int j = part.indexOf('=');
                if (j >= 0) {
                    String key = part.substring(0, j);
                    String value = part.substring(j + 1);
                    parameters.put(key, value);
                    // compatible with lower versions registering "default." keys
                    if (key.startsWith(DEFAULT_KEY_PREFIX)) {
                        parameters.putIfAbsent(key.substring(DEFAULT_KEY_PREFIX.length()), value);
                    }
                } else {
                    parameters.put(part, part);
                }
            }
        }
        URLParam urlParam = new URLParam(parameters);
        urlParam.setRawParam(rawParam);
        return urlParam;
    }

    private String rawParam;
    private Map<String, String> params;

    public URLParam() {
    }

    public URLParam(Map<String, String> params) {
        this.params = params;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getRawParam() {
        return rawParam;
    }

    public void setRawParam(String rawParam) {
        this.rawParam = rawParam;
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof URLParam)) return false;
        URLParam that = (URLParam) obj;
        return Objects.equals(this.getParams(), that.getParams());
    }

    @Override
    public String toString() {
        return params.toString();
    }
}
