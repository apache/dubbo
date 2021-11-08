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
import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

public class ServiceConfigURL extends URL {

    private volatile transient String full;
    private volatile transient String string;
    private volatile transient String identity;
    private volatile transient String parameter;

    public ServiceConfigURL() {
        super();
    }

    public ServiceConfigURL(URLAddress urlAddress, URLParam urlParam, Map<String, Object> attributes) {
        super(urlAddress, urlParam, attributes);
    }


    public ServiceConfigURL(String protocol, String host, int port) {
        this(protocol, null, null, host, port, null, (Map<String, String>) null);
    }

    public ServiceConfigURL(String protocol, String host, int port, String[] pairs) { // varargs ... conflict with the following path argument, use array instead.
        this(protocol, null, null, host, port, null, CollectionUtils.toStringMap(pairs));
    }

    public ServiceConfigURL(String protocol, String host, int port, Map<String, String> parameters) {
        this(protocol, null, null, host, port, null, parameters);
    }

    public ServiceConfigURL(String protocol, String host, int port, String path) {
        this(protocol, null, null, host, port, path, (Map<String, String>) null);
    }

    public ServiceConfigURL(String protocol, String host, int port, String path, String... pairs) {
        this(protocol, null, null, host, port, path, CollectionUtils.toStringMap(pairs));
    }

    public ServiceConfigURL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this(protocol, null, null, host, port, path, parameters);
    }

    public ServiceConfigURL(String protocol, String username, String password, String host, int port, String path) {
        this(protocol, username, password, host, port, path, (Map<String, String>) null);
    }

    public ServiceConfigURL(String protocol, String username, String password, String host, int port, String path, String... pairs) {
        this(protocol, username, password, host, port, path, CollectionUtils.toStringMap(pairs));
    }

    public ServiceConfigURL(String protocol,
               String username,
               String password,
               String host,
               int port,
               String path,
               Map<String, String> parameters) {
        this(new PathURLAddress(protocol, username, password, path, host, port), URLParam.parse(parameters), null);
    }

    public ServiceConfigURL(String protocol,
                            String username,
                            String password,
                            String host,
                            int port,
                            String path,
                            Map<String, String> parameters,
                            Map<String, Object> attributes) {
        this(new PathURLAddress(protocol, username, password, path, host, port), URLParam.parse(parameters), attributes);
    }

    @Override
    protected <T extends URL> T newURL(URLAddress urlAddress, URLParam urlParam) {
        return (T) new ServiceConfigURL(urlAddress, urlParam, attributes);
    }

    @Override
    public URL addAttributes(Map<String, Object> attributes) {
        Map<String, Object> newAttributes = new HashMap<>();
        if (this.attributes != null) {
            newAttributes.putAll(this.attributes);
        }
        newAttributes.putAll(attributes);
        return new ServiceConfigURL(getUrlAddress(), getUrlParam(), newAttributes);
    }

    @Override
    public ServiceConfigURL putAttribute(String key, Object obj) {
        Map<String, Object> newAttributes = new HashMap<>();
        if (attributes != null) {
            newAttributes.putAll(attributes);
        }
        newAttributes.put(key, obj);
        return new ServiceConfigURL(getUrlAddress(), getUrlParam(), newAttributes);
    }

    @Override
    public URL removeAttribute(String key) {
        Map<String, Object> newAttributes = new HashMap<>();
        if (attributes != null) {
            newAttributes.putAll(attributes);
        }
        newAttributes.remove(key);
        return new ServiceConfigURL(getUrlAddress(), getUrlParam(), newAttributes);
    }

    @Override
    public String toString() {
        if (string != null) {
            return string;
        }
        return string = super.toString();
    }

    @Override
    public String toFullString() {
        if (full != null) {
            return full;
        }
        return full = super.toFullString();
    }

    @Override
    public String toIdentityString() {
        if (identity != null) {
            return identity;
        }
        return identity = super.toIdentityString();
    }

    @Override
    public String toParameterString() {
        if (parameter != null) {
            return parameter;
        }
        return parameter = super.toParameterString();
    }
}
