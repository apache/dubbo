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
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ServiceConfigURL extends URL {


    private volatile transient ConcurrentMap<String, URL> urls;
    private volatile transient ConcurrentMap<String, Number> numbers;
    private volatile transient ConcurrentMap<String, Map<String, Number>> methodNumbers;
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


    @Override
    public URL getUrlParameter(String key) {
        URL u = getUrls().get(key);
        if (u != null) {
            return u;
        }
        String value = getParameterAndDecoded(key);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        u = URL.valueOf(value);
        getUrls().put(key, u);
        return u;
    }

    @Override
    public double getParameter(String key, double defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.doubleValue();
        }
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        double d = Double.parseDouble(value);
        getNumbers().put(key, d);
        return d;
    }

    @Override
    public float getParameter(String key, float defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.floatValue();
        }
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        float f = Float.parseFloat(value);
        getNumbers().put(key, f);
        return f;
    }

    @Override
    public long getParameter(String key, long defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.longValue();
        }
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        getNumbers().put(key, l);
        return l;
    }

    @Override
    public int getParameter(String key, int defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.intValue();
        }
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(key, i);
        return i;
    }

    @Override
    public short getParameter(String key, short defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.shortValue();
        }
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        short s = Short.parseShort(value);
        getNumbers().put(key, s);
        return s;
    }

    @Override
    public byte getParameter(String key, byte defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.byteValue();
        }
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        byte b = Byte.parseByte(value);
        getNumbers().put(key, b);
        return b;
    }

    @Override
    public double getMethodParameter(String method, String key, double defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.doubleValue();
        }
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        double d = Double.parseDouble(value);
        updateCachedNumber(method, key, d);
        return d;
    }

    @Override
    public float getMethodParameter(String method, String key, float defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.floatValue();
        }
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        float f = Float.parseFloat(value);
        updateCachedNumber(method, key, f);
        return f;
    }

    @Override
    public long getMethodParameter(String method, String key, long defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.longValue();
        }
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        updateCachedNumber(method, key, l);
        return l;
    }

    @Override
    public int getMethodParameter(String method, String key, int defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.intValue();
        }
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        updateCachedNumber(method, key, i);
        return i;
    }

    @Override
    public short getMethodParameter(String method, String key, short defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.shortValue();
        }
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        short s = Short.parseShort(value);
        updateCachedNumber(method, key, s);
        return s;
    }

    @Override
    public byte getMethodParameter(String method, String key, byte defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.byteValue();
        }
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        byte b = Byte.parseByte(value);
        updateCachedNumber(method, key, b);
        return b;
    }

    @Override
    public double getServiceParameter(String service, String key, double defaultValue) {
        Number n = getServiceNumbers(service).get(key);
        if (n != null) {
            return n.doubleValue();
        }
        String value = getServiceParameter(service, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        double d = Double.parseDouble(value);
        getNumbers().put(key, d);
        return d;
    }

    @Override
    public float getServiceParameter(String service, String key, float defaultValue) {
        Number n = getServiceNumbers(service).get(key);
        if (n != null) {
            return n.floatValue();
        }
        String value = getServiceParameter(service, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        float f = Float.parseFloat(value);
        getNumbers().put(key, f);
        return f;
    }

    @Override
    public long getServiceParameter(String service, String key, long defaultValue) {
        Number n = getServiceNumbers(service).get(key);
        if (n != null) {
            return n.longValue();
        }
        String value = getServiceParameter(service, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        getNumbers().put(key, l);
        return l;
    }

    @Override
    public short getServiceParameter(String service, String key, short defaultValue) {
        Number n = getServiceNumbers(service).get(key);
        if (n != null) {
            return n.shortValue();
        }
        String value = getServiceParameter(service, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        short s = Short.parseShort(value);
        getNumbers().put(key, s);
        return s;
    }

    @Override
    public byte getServiceParameter(String service, String key, byte defaultValue) {
        Number n = getServiceNumbers(service).get(key);
        if (n != null) {
            return n.byteValue();
        }
        String value = getServiceParameter(service, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        byte b = Byte.parseByte(value);
        getNumbers().put(key, b);
        return b;
    }

    @Override
    public double getServiceMethodParameter(String service, String method, String key, double defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.doubleValue();
        }
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        double d = Double.parseDouble(value);
        updateCachedNumber(method, key, d);
        return d;
    }

    @Override
    public float getServiceMethodParameter(String service, String method, String key, float defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.floatValue();
        }
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        float f = Float.parseFloat(value);
        updateCachedNumber(method, key, f);
        return f;
    }

    @Override
    public long getServiceMethodParameter(String service, String method, String key, long defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.longValue();
        }
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        updateCachedNumber(method, key, l);
        return l;
    }

    @Override
    public int getServiceMethodParameter(String service, String method, String key, int defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.intValue();
        }
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        updateCachedNumber(method, key, i);
        return i;
    }

    @Override
    public short getServiceMethodParameter(String service, String method, String key, short defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.shortValue();
        }
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        short s = Short.parseShort(value);
        updateCachedNumber(method, key, s);
        return s;
    }

    @Override
    public byte getServiceMethodParameter(String service, String method, String key, byte defaultValue) {
        Number n = getCachedNumber(method, key);
        if (n != null) {
            return n.byteValue();
        }
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        byte b = Byte.parseByte(value);
        updateCachedNumber(method, key, b);
        return b;
    }


    private Map<String, URL> getUrls() {
        // concurrent initialization is tolerant
        if (urls == null) {
            urls = new ConcurrentHashMap<>();
        }
        return urls;
    }

    protected Map<String, Number> getNumbers() {
        // concurrent initialization is tolerant
        if (numbers == null) {
            numbers = new ConcurrentHashMap<>();
        }
        return numbers;
    }

    private Number getCachedNumber(String method, String key) {
        Map<String, Number> keyNumber = getMethodNumbers().get(method);
        if (keyNumber != null) {
            return keyNumber.get(key);
        }
        return null;
    }

    private void updateCachedNumber(String method, String key, Number n) {
        Map<String, Number> keyNumber = ConcurrentHashMapUtils.computeIfAbsent(getMethodNumbers(), method, m -> new HashMap<>());
        keyNumber.put(key, n);
    }

    protected ConcurrentMap<String, Map<String, Number>> getMethodNumbers() {
        if (methodNumbers == null) { // concurrent initialization is tolerant
            methodNumbers = new ConcurrentHashMap<>();
        }
        return methodNumbers;
    }

    protected Map<String, Number> getServiceNumbers(String service) {
        return getNumbers();
    }

    protected Map<String, Map<String, Number>> getServiceMethodNumbers(String service) {
        return getMethodNumbers();
    }

}
