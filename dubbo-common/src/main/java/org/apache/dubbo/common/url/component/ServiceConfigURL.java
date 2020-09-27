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

import org.apache.dubbo.common.URL;

import java.util.HashMap;
import java.util.Map;

public class ServiceConfigURL extends URL {
    private Map<String, Object> attributes;

    public ServiceConfigURL(String protocol,
                            String username,
                            String password,
                            String host,
                            int port,
                            String path) {
        this(protocol, username, password, host, port, path, null);
    }

    public ServiceConfigURL(String protocol,
                            String username,
                            String password,
                            String host,
                            int port,
                            String path,
                            Map<String, String> parameters) {
        this(protocol, username, password, host, port, path, parameters, true);
    }

    public ServiceConfigURL(String protocol,
                            String username,
                            String password,
                            String host,
                            int port,
                            String path,
                            Map<String, String> parameters,
                            boolean modifiable) {
       super(protocol, username, password, host, port, path, parameters, modifiable);
       this.attributes = new HashMap<>();
    }

    public ServiceConfigURL(URLAddress urlAddress, URLParam urlParam) {
        super(urlAddress, urlParam);
        this.attributes = new HashMap<>();
    }

    protected <T extends URL> T newURL(URLAddress urlAddress, URLParam urlParam) {
        return (T) new ServiceConfigURL(urlAddress, urlParam);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object obj) {
        attributes.put(key, obj);
    }

    public static ServiceConfigURL valueOf(String rawURL) {
        URL url = URL.valueOf(rawURL);
        return new ServiceConfigURL(url.getUrlAddress(), url.getUrlParam());
    }
}
