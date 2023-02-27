<<<<<<< HEAD
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
package org.apache.dubbo.common;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.InmemoryConfiguration;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PASSWORD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.USERNAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.convert.Converter.convertIfPossible;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

/**
 * URL - Uniform Resource Locator (Immutable, ThreadSafe)
 * <p>
 * url example:
 * <ul>
 * <li>http://www.facebook.com/friends?param1=value1&amp;param2=value2
 * <li>http://username:password@10.20.130.230:8080/list?version=1.0.0
 * <li>ftp://username:password@192.168.1.7:21/1/read.txt
 * <li>registry://192.168.1.7:9090/org.apache.dubbo.service1?param1=value1&amp;param2=value2
 * </ul>
 * <p>
 * Some strange example below:
 * <ul>
 * <li>192.168.1.3:20880<br>
 * for this case, url protocol = null, url host = 192.168.1.3, port = 20880, url path = null
 * <li>file:///home/user1/router.js?type=script<br>
 * for this case, url protocol = file, url host = null, url path = home/user1/router.js
 * <li>file://home/user1/router.js?type=script<br>
 * for this case, url protocol = file, url host = home, url path = user1/router.js
 * <li>file:///D:/1/router.js?type=script<br>
 * for this case, url protocol = file, url host = null, url path = D:/1/router.js
 * <li>file:/D:/1/router.js?type=script<br>
 * same as above file:///D:/1/router.js?type=script
 * <li>/home/user1/router.js?type=script <br>
 * for this case, url protocol = null, url host = null, url path = home/user1/router.js
 * <li>home/user1/router.js?type=script <br>
 * for this case, url protocol = null, url host = home, url path = user1/router.js
 * </ul>
 *
 * @see java.net.URL
 * @see java.net.URI
 */
public /*final**/
class URL implements Serializable {

    private static final long serialVersionUID = -1985165475234910535L;

    protected String protocol;

    protected String username;

    protected String password;

    // by default, host to registry
    protected String host;

    // by default, port to registry
    protected int port;

    protected String path;

    private final Map<String, String> parameters;

    private final Map<String, Map<String, String>> methodParameters;

    // ==== cache ====

    private volatile transient Map<String, Number> numbers;

    private volatile transient Map<String, Map<String, Number>> methodNumbers;

    private volatile transient Map<String, URL> urls;

    private volatile transient String ip;

    private volatile transient String full;

    private volatile transient String identity;

    private volatile transient String parameter;

    private volatile transient String string;

    private transient String serviceKey;
    private transient String protocolServiceKey;

    private transient String address;

    protected URL() {
        this.protocol = null;
        this.username = null;
        this.password = null;
        this.host = null;
        this.port = 0;
        this.address = null;
        this.path = null;
        this.parameters = null;
        this.methodParameters = null;
    }

    public URL(String protocol, String host, int port) {
        this(protocol, null, null, host, port, null, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, String[] pairs) { // varargs ... conflict with the following path argument, use array instead.
        this(protocol, null, null, host, port, null, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String host, int port, Map<String, String> parameters) {
        this(protocol, null, null, host, port, null, parameters);
    }

    public URL(String protocol, String host, int port, String path) {
        this(protocol, null, null, host, port, path, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, String path, String... pairs) {
        this(protocol, null, null, host, port, path, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this(protocol, null, null, host, port, path, parameters);
    }

    public URL(String protocol, String username, String password, String host, int port, String path) {
        this(protocol, username, password, host, port, path, (Map<String, String>) null);
    }

    public URL(String protocol, String username, String password, String host, int port, String path, String... pairs) {
        this(protocol, username, password, host, port, path, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol,
               String username,
               String password,
               String host,
               int port,
               String path,
               Map<String, String> parameters) {
        this(protocol, username, password, host, port, path, parameters, toMethodParameters(parameters));
    }

    public URL(String protocol,
               String username,
               String password,
               String host,
               int port,
               String path,
               Map<String, String> parameters,
               Map<String, Map<String, String>> methodParameters) {
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = Math.max(port, 0);
        this.address = getAddress(this.host, this.port);

        // trim the beginning "/"
        while (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        this.path = path;
        if (parameters == null) {
            parameters = new HashMap<>();
        } else {
            parameters = new HashMap<>(parameters);
        }
        this.parameters = Collections.unmodifiableMap(parameters);
        this.methodParameters = Collections.unmodifiableMap(methodParameters);
    }

    private static String getAddress(String host, int port) {
        return port <= 0 ? host : host + ':' + port;
    }

    /**
     * NOTICE: This method allocate too much objects, we can use {@link URLStrParser#parseDecodedStr(String)} instead.
     * <p>
     * Parse url string
     *
     * @param url URL string
     * @return URL instance
     * @see URL
     */
    public static URL valueOf(String url) {
        if (url == null || (url = url.trim()).length() == 0) {
            throw new IllegalArgumentException("url == null");
        }
        String protocol = null;
        String username = null;
        String password = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = null;
        int i = url.indexOf('?'); // separator between body and parameters
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("[&;]");
            parameters = new HashMap<>();
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
            url = url.substring(0, i);
        }

        // ignore the url content following '#'
        int poundIndex = url.indexOf('#');
        if (poundIndex != -1) {
            url = url.substring(0, poundIndex);
        }

        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0) {
                throw new IllegalStateException("url missing protocol: \"" + url + "\"");
            }
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            // case: file:/path/to/file.txt
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) {
                    throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                }
                protocol = url.substring(0, i);
                url = url.substring(i + 1);
            }
        }

        i = url.indexOf('/');
        if (i >= 0) {
            path = url.substring(i + 1);
            url = url.substring(0, i);
        }
        i = url.lastIndexOf('@');
        if (i >= 0) {
            username = url.substring(0, i);
            int j = username.indexOf(':');
            if (j >= 0) {
                password = username.substring(j + 1);
                username = username.substring(0, j);
            }
            url = url.substring(i + 1);
        }
        i = url.lastIndexOf(':');
        if (i >= 0 && i < url.length() - 1) {
            if (url.lastIndexOf('%') > i) {
                // ipv6 address with scope id
                // e.g. fe80:0:0:0:894:aeec:f37d:23e1%en0
                // see https://howdoesinternetwork.com/2013/ipv6-zone-id
                // ignore
            } else {
                port = Integer.parseInt(url.substring(i + 1));
                url = url.substring(0, i);
            }
        }
        if (url.length() > 0) {
            host = url;
        }

        return new URL(protocol, username, password, host, port, path, parameters);
    }

    public static Map<String, Map<String, String>> toMethodParameters(Map<String, String> parameters) {
        Map<String, Map<String, String>> methodParameters = new HashMap<>();
        if (parameters == null) {
            return methodParameters;
        }

        String methodsString = parameters.get(METHODS_KEY);
        if (StringUtils.isNotEmpty(methodsString)) {
            List<String> methods = StringUtils.splitToList(methodsString, ',');
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey();
                for (int i = 0; i < methods.size(); i++) {
                    String method = methods.get(i);
                    int methodLen = method.length();
                    if (key.length() > methodLen
                            && key.startsWith(method)
                            && key.charAt(methodLen) == '.') {//equals to: key.startsWith(method + '.')
                        String realKey = key.substring(methodLen + 1);
                        URL.putMethodParameter(method, realKey, entry.getValue(), methodParameters);
                    }
                }
            }
        } else {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey();
                int methodSeparator = key.indexOf('.');
                if (methodSeparator > 0) {
                    String method = key.substring(0, methodSeparator);
                    String realKey = key.substring(methodSeparator + 1);
                    URL.putMethodParameter(method, realKey, entry.getValue(), methodParameters);
                }
            }
        }
        return methodParameters;
    }

    public static URL valueOf(String url, String... reserveParams) {
        URL result = valueOf(url);
        if (reserveParams == null || reserveParams.length == 0) {
            return result;
        }
        Map<String, String> newMap = new HashMap<>(reserveParams.length);
        Map<String, String> oldMap = result.getParameters();
        for (String reserveParam : reserveParams) {
            String tmp = oldMap.get(reserveParam);
            if (StringUtils.isNotEmpty(tmp)) {
                newMap.put(reserveParam, tmp);
            }
        }
        return result.clearParameters().addParameters(newMap);
    }

    public static URL valueOf(URL url, String[] reserveParams, String[] reserveParamPrefixs) {
        Map<String, String> newMap = new HashMap<>();
        Map<String, String> oldMap = url.getParameters();
        if (reserveParamPrefixs != null && reserveParamPrefixs.length != 0) {
            for (Map.Entry<String, String> entry : oldMap.entrySet()) {
                for (String reserveParamPrefix : reserveParamPrefixs) {
                    if (entry.getKey().startsWith(reserveParamPrefix) && StringUtils.isNotEmpty(entry.getValue())) {
                        newMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        if (reserveParams != null) {
            for (String reserveParam : reserveParams) {
                String tmp = oldMap.get(reserveParam);
                if (StringUtils.isNotEmpty(tmp)) {
                    newMap.put(reserveParam, tmp);
                }
            }
        }
        return newMap.isEmpty() ? new URL(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), url.getPath())
                : new URL(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), url.getPath(), newMap);
    }

    public static String encode(String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String decode(String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    static String appendDefaultPort(String address, int defaultPort) {
        if (address != null && address.length() > 0 && defaultPort > 0) {
            int i = address.indexOf(':');
            if (i < 0) {
                return address + ":" + defaultPort;
            } else if (Integer.parseInt(address.substring(i + 1)) == 0) {
                return address.substring(0, i + 1) + defaultPort;
            }
        }
        return address;
    }

    public String getProtocol() {
        return protocol;
    }

    public URL setProtocol(String protocol) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getUsername() {
        return username;
    }

    public URL setUsername(String username) {
        return new URL(getProtocol(), username, password, host, port, path, getParameters());
    }

    public String getPassword() {
        return password;
    }

    public URL setPassword(String password) {
        return new URL(getProtocol(), username, password, host, port, path, getParameters());
    }

    public String getAuthority() {
        if (StringUtils.isEmpty(username)
                && StringUtils.isEmpty(password)) {
            return null;
        }
        return (username == null ? "" : username)
                + ":" + (password == null ? "" : password);
    }

    public String getHost() {
        return host;
    }

    public URL setHost(String host) {
        return new URL(getProtocol(), username, password, host, port, path, getParameters());
    }

    /**
     * Fetch IP address for this URL.
     * <p>
     * Pls. note that IP should be used instead of Host when to compare with socket's address or to search in a map
     * which use address as its key.
     *
     * @return ip in string format
     */
    public String getIp() {
        if (ip == null) {
            ip = NetUtils.getIpByHost(host);
        }
        return ip;
    }

    public int getPort() {
        return port;
    }

    public URL setPort(int port) {
        return new URL(getProtocol(), username, password, host, port, path, getParameters());
    }

    public int getPort(int defaultPort) {
        return port <= 0 ? defaultPort : port;
    }

    public String getAddress() {
        if (address == null) {
            address = getAddress(host, port);
        }
        return address;
    }

    public URL setAddress(String address) {
        int i = address.lastIndexOf(':');
        String host;
        int port = this.port;
        if (i >= 0) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
        }
        return new URL(getProtocol(), username, password, host, port, path, getParameters());
    }

    public String getBackupAddress() {
        return getBackupAddress(0);
    }

    public String getBackupAddress(int defaultPort) {
        StringBuilder address = new StringBuilder(appendDefaultPort(getAddress(), defaultPort));
        String[] backups = getParameter(RemotingConstants.BACKUP_KEY, new String[0]);
        if (ArrayUtils.isNotEmpty(backups)) {
            for (String backup : backups) {
                address.append(',');
                address.append(appendDefaultPort(backup, defaultPort));
            }
        }
        return address.toString();
    }

    public List<URL> getBackupUrls() {
        List<URL> urls = new ArrayList<>();
        urls.add(this);
        String[] backups = getParameter(RemotingConstants.BACKUP_KEY, new String[0]);
        if (backups != null && backups.length > 0) {
            for (String backup : backups) {
                urls.add(this.setAddress(backup));
            }
        }
        return urls;
    }

    public String getPath() {
        return path;
    }

    public URL setPath(String path) {
        return new URL(getProtocol(), username, password, host, port, path, getParameters());
    }

    public String getAbsolutePath() {
        if (path != null && !path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Get the parameters to be selected(filtered)
     *
     * @param nameToSelect the {@link Predicate} to select the parameter name
     * @return non-null {@link Map}
     * @since 2.7.8
     */
    public Map<String, String> getParameters(Predicate<String> nameToSelect) {
        Map<String, String> selectedParameters = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : getParameters().entrySet()) {
            String name = entry.getKey();
            if (nameToSelect.test(name)) {
                selectedParameters.put(name, entry.getValue());
            }
        }
        return Collections.unmodifiableMap(selectedParameters);
    }

    /**
     * Get parameter
     *
     * @param key       the key of parameter
     * @param valueType the type of parameter value
     * @param <T>       the type of parameter value
     * @return get the parameter if present, or <code>null</code>
     * @since 2.7.8
     */
    public <T> T getParameter(String key, Class<T> valueType) {
        return getParameter(key, valueType, null);
    }

    /**
     * Get parameter
     *
     * @param key          the key of parameter
     * @param valueType    the type of parameter value
     * @param defaultValue the default value if parameter is absent
     * @param <T>          the type of parameter value
     * @return get the parameter if present, or <code>defaultValue</code> will be used.
     * @since 2.7.8
     */
    public <T> T getParameter(String key, Class<T> valueType, T defaultValue) {
        String value = getParameter(key);
        T result = null;
        if (!isBlank(value)) {
            result = convertIfPossible(value, valueType);
        }
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }


    public Map<String, Map<String, String>> getMethodParameters() {
        return methodParameters;
    }

    public String getParameterAndDecoded(String key) {
        return getParameterAndDecoded(key, null);
    }

    public String getParameterAndDecoded(String key, String defaultValue) {
        return decode(getParameter(key, defaultValue));
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public String getParameter(String key, String defaultValue) {
        String value = getParameter(key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    public String[] getParameter(String key, String[] defaultValue) {
        String value = getParameter(key);
        return StringUtils.isEmpty(value) ? defaultValue : COMMA_SPLIT_PATTERN.split(value);
    }

    public List<String> getParameter(String key, List<String> defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        String[] strArray = COMMA_SPLIT_PATTERN.split(value);
        return Arrays.asList(strArray);
    }

    protected Map<String, Number> getNumbers() {
        // concurrent initialization is tolerant
        if (numbers == null) {
            numbers = new ConcurrentHashMap<>();
        }
        return numbers;
    }

    protected Map<String, Map<String, Number>> getMethodNumbers() {
        if (methodNumbers == null) { // concurrent initialization is tolerant
            methodNumbers = new ConcurrentHashMap<>();
        }
        return methodNumbers;
    }

    private Map<String, URL> getUrls() {
        // concurrent initialization is tolerant
        if (urls == null) {
            urls = new ConcurrentHashMap<>();
        }
        return urls;
    }

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

    public float getPositiveParameter(String key, float defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        float value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public double getPositiveParameter(String key, double defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        double value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public long getPositiveParameter(String key, long defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        long value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public int getPositiveParameter(String key, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        int value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public short getPositiveParameter(String key, short defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        short value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public byte getPositiveParameter(String key, byte defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        byte value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public char getParameter(String key, char defaultValue) {
        String value = getParameter(key);
        return StringUtils.isEmpty(value) ? defaultValue : value.charAt(0);
    }

    public boolean getParameter(String key, boolean defaultValue) {
        String value = getParameter(key);
        return StringUtils.isEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    public boolean hasParameter(String key) {
        String value = getParameter(key);
        return value != null && value.length() > 0;
    }

    public String getMethodParameterAndDecoded(String method, String key) {
        return URL.decode(getMethodParameter(method, key));
    }

    public String getMethodParameterAndDecoded(String method, String key, String defaultValue) {
        return URL.decode(getMethodParameter(method, key, defaultValue));
    }

    public String getMethodParameterStrict(String method, String key) {
        Map<String, String> keyMap = getMethodParameters().get(method);
        String value = null;
        if (keyMap != null) {
            value = keyMap.get(key);
        }
        return value;
    }

    public String getMethodParameter(String method, String key) {
        Map<String, String> keyMap = getMethodParameters().get(method);
        String value = null;
        if (keyMap != null) {
            value = keyMap.get(key);
        }
        if (StringUtils.isEmpty(value)) {
            value = parameters.get(key);
        }
        return value;
    }

    public String getMethodParameter(String method, String key, String defaultValue) {
        String value = getMethodParameter(method, key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

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

    private Number getCachedNumber(String method, String key) {
        Map<String, Number> keyNumber = getMethodNumbers().get(method);
        if (keyNumber != null) {
            return keyNumber.get(key);
        }
        return null;
    }

    private void updateCachedNumber(String method, String key, Number n) {
        Map<String, Number> keyNumber = getMethodNumbers().computeIfAbsent(method, m -> new HashMap<>());
        keyNumber.put(key, n);
    }

    public double getMethodPositiveParameter(String method, String key, double defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        double value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public float getMethodPositiveParameter(String method, String key, float defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        float value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public long getMethodPositiveParameter(String method, String key, long defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        long value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public int getMethodPositiveParameter(String method, String key, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        int value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public short getMethodPositiveParameter(String method, String key, short defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        short value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public byte getMethodPositiveParameter(String method, String key, byte defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        byte value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public char getMethodParameter(String method, String key, char defaultValue) {
        String value = getMethodParameter(method, key);
        return StringUtils.isEmpty(value) ? defaultValue : value.charAt(0);
    }

    public boolean getMethodParameter(String method, String key, boolean defaultValue) {
        String value = getMethodParameter(method, key);
        return StringUtils.isEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    public boolean hasMethodParameter(String method, String key) {
        if (method == null) {
            String suffix = "." + key;
            for (String fullKey : parameters.keySet()) {
                if (fullKey.endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        }
        if (key == null) {
            String prefix = method + ".";
            for (String fullKey : parameters.keySet()) {
                if (fullKey.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
        String value = getMethodParameterStrict(method, key);
        return StringUtils.isNotEmpty(value);
    }

    public boolean hasMethodParameter(String method) {
        if (method == null) {
            return false;
        }
        return getMethodParameters().containsKey(method);
    }

    public boolean isLocalHost() {
        return NetUtils.isLocalHost(host) || getParameter(LOCALHOST_KEY, false);
    }

    public boolean isAnyHost() {
        return ANYHOST_VALUE.equals(host) || getParameter(ANYHOST_KEY, false);
    }

    public URL addParameterAndEncoded(String key, String value) {
        if (StringUtils.isEmpty(value)) {
            return this;
        }
        return addParameter(key, encode(value));
    }

    public URL addParameter(String key, boolean value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, char value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, byte value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, short value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, int value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, long value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, float value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, double value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, Enum<?> value) {
        if (value == null) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, Number value) {
        if (value == null) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, CharSequence value) {
        if (value == null || value.length() == 0) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, String value) {
        if (StringUtils.isEmpty(key)
                || StringUtils.isEmpty(value)) {
            return this;
        }
        // if value doesn't change, return immediately
        if (value.equals(getParameters().get(key))) { // value != null
            return this;
        }

        Map<String, String> map = new HashMap<>(getParameters());
        map.put(key, value);

        return new URL(getProtocol(), username, password, host, port, path, map);
    }

    public URL addParameterIfAbsent(String key, String value) {
        if (StringUtils.isEmpty(key)
                || StringUtils.isEmpty(value)) {
            return this;
        }
        if (hasParameter(key)) {
            return this;
        }
        Map<String, String> map = new HashMap<>(getParameters());
        map.put(key, value);

        return new URL(getProtocol(), username, password, host, port, path, map);
    }

    public URL addMethodParameter(String method, String key, String value) {
        if (StringUtils.isEmpty(method)
                || StringUtils.isEmpty(key)
                || StringUtils.isEmpty(value)) {
            return this;
        }

        Map<String, String> map = new HashMap<>(getParameters());
        map.put(method + "." + key, value);
        Map<String, Map<String, String>> methodMap = toMethodParameters(map);
        URL.putMethodParameter(method, key, value, methodMap);

        return new URL(getProtocol(), username, password, host, port, path, map, methodMap);
    }

    public URL addMethodParameterIfAbsent(String method, String key, String value) {
        if (StringUtils.isEmpty(method)
                || StringUtils.isEmpty(key)
                || StringUtils.isEmpty(value)) {
            return this;
        }
        if (hasMethodParameter(method, key)) {
            return this;
        }

        Map<String, String> map = new HashMap<>(getParameters());
        map.put(method + "." + key, value);
        Map<String, Map<String, String>> methodMap = toMethodParameters(map);
        URL.putMethodParameter(method, key, value, methodMap);

        return new URL(getProtocol(), username, password, host, port, path, map, methodMap);
    }

    /**
     * Add parameters to a new url.
     *
     * @param parameters parameters in key-value pairs
     * @return A new URL
     */
    public URL addParameters(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return this;
        }

        boolean hasAndEqual = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String value = getParameters().get(entry.getKey());
            if (value == null) {
                if (entry.getValue() != null) {
                    hasAndEqual = false;
                    break;
                }
            } else {
                if (!value.equals(entry.getValue())) {
                    hasAndEqual = false;
                    break;
                }
            }
        }
        // return immediately if there's no change
        if (hasAndEqual) {
            return this;
        }

        Map<String, String> map = new HashMap<>(getParameters());
        map.putAll(parameters);
        return new URL(getProtocol(), username, password, host, port, path, map);
    }

    public URL addParametersIfAbsent(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return this;
        }
        Map<String, String> map = new HashMap<>(parameters);
        map.putAll(getParameters());
        return new URL(getProtocol(), username, password, host, port, path, map);
    }

    public URL addParameters(String... pairs) {
        if (ArrayUtils.isEmpty(pairs)) {
            return this;
        }
        return addParameters(CollectionUtils.toStringMap(pairs));
    }

    public URL addParameterString(String query) {
        if (StringUtils.isEmpty(query)) {
            return this;
        }
        return addParameters(StringUtils.parseQueryString(query));
    }

    public URL removeParameter(String key) {
        if (StringUtils.isEmpty(key)) {
            return this;
        }
        return removeParameters(key);
    }

    public URL removeParameters(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return this;
        }
        return removeParameters(keys.toArray(new String[0]));
    }

    public URL removeParameters(String... keys) {
        if (keys == null || keys.length == 0) {
            return this;
        }
        Map<String, String> map = new HashMap<>(getParameters());
        for (String key : keys) {
            map.remove(key);
        }
        if (map.size() == getParameters().size()) {
            return this;
        }
        return new URL(getProtocol(), username, password, host, port, path, map);
    }

    public URL clearParameters() {
        return new URL(getProtocol(), username, password, host, port, path, new HashMap<>());
    }

    public String getRawParameter(String key) {
        if (PROTOCOL_KEY.equals(key)) {
            return protocol;
        }
        if (USERNAME_KEY.equals(key)) {
            return username;
        }
        if (PASSWORD_KEY.equals(key)) {
            return password;
        }
        if (HOST_KEY.equals(key)) {
            return host;
        }
        if (PORT_KEY.equals(key)) {
            return String.valueOf(port);
        }
        if (PATH_KEY.equals(key)) {
            return path;
        }
        return getParameter(key);
    }

    public Map<String, String> toMap() {
        Map<String, String> map = null == parameters ? new HashMap<>() : new HashMap<>(parameters);

        if (protocol != null) {
            map.put(PROTOCOL_KEY, protocol);
        }
        if (username != null) {
            map.put(USERNAME_KEY, username);
        }
        if (password != null) {
            map.put(PASSWORD_KEY, password);
        }
        if (host != null) {
            map.put(HOST_KEY, host);
        }
        if (port > 0) {
            map.put(PORT_KEY, String.valueOf(port));
        }
        if (path != null) {
            map.put(PATH_KEY, path);
        }
        return map;
    }

    @Override
    public String toString() {
        if (string != null) {
            return string;
        }
        return string = buildString(false, true); // no show username and password
    }

    public String toString(String... parameters) {
        return buildString(false, true, parameters); // no show username and password
    }

    public String toIdentityString() {
        if (identity != null) {
            return identity;
        }
        return identity = buildString(true, false); // only return identity message, see the method "equals" and "hashCode"
    }

    public String toIdentityString(String... parameters) {
        return buildString(true, false, parameters); // only return identity message, see the method "equals" and "hashCode"
    }

    public String toFullString() {
        if (full != null) {
            return full;
        }
        return full = buildString(true, true);
    }

    public String toFullString(String... parameters) {
        return buildString(true, true, parameters);
    }

    public String toParameterString() {
        if (parameter != null) {
            return parameter;
        }
        return parameter = toParameterString(new String[0]);
    }

    public String toParameterString(String... parameters) {
        StringBuilder buf = new StringBuilder();
        buildParameters(buf, false, parameters);
        return buf.toString();
    }

    private void buildParameters(StringBuilder buf, boolean concat, String[] parameters) {
        if (CollectionUtils.isNotEmptyMap(getParameters())) {
            List<String> includes = (ArrayUtils.isEmpty(parameters) ? null : Arrays.asList(parameters));
            boolean first = true;
            for (Map.Entry<String, String> entry : new TreeMap<>(getParameters()).entrySet()) {
                if (StringUtils.isNotEmpty(entry.getKey())
                        && (includes == null || includes.contains(entry.getKey()))) {
                    if (first) {
                        if (concat) {
                            buf.append("?");
                        }
                        first = false;
                    } else {
                        buf.append("&");
                    }
                    buf.append(entry.getKey());
                    buf.append("=");
                    buf.append(entry.getValue() == null ? "" : entry.getValue().trim());
                }
            }
        }
    }

    private String buildString(boolean appendUser, boolean appendParameter, String... parameters) {
        return buildString(appendUser, appendParameter, false, false, parameters);
    }

    private String buildString(boolean appendUser, boolean appendParameter, boolean useIP, boolean useService, String... parameters) {
        StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotEmpty(protocol)) {
            buf.append(protocol);
            buf.append("://");
        }
        if (appendUser && StringUtils.isNotEmpty(username)) {
            buf.append(username);
            if (StringUtils.isNotEmpty(password)) {
                buf.append(":");
                buf.append(password);
            }
            buf.append("@");
        }
        String host;
        if (useIP) {
            host = getIp();
        } else {
            host = getHost();
        }
        if (StringUtils.isNotEmpty(host)) {
            buf.append(host);
            if (port > 0) {
                buf.append(":");
                buf.append(port);
            }
        }
        String path;
        if (useService) {
            path = getServiceKey();
        } else {
            path = getPath();
        }
        if (StringUtils.isNotEmpty(path)) {
            buf.append("/");
            buf.append(path);
        }

        if (appendParameter) {
            buildParameters(buf, true, parameters);
        }
        return buf.toString();
    }

    public java.net.URL toJavaURL() {
        try {
            return new java.net.URL(toString());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public InetSocketAddress toInetSocketAddress() {
        return new InetSocketAddress(host, port);
    }

    /**
     * The format is "{interface}:[version]:[group]"
     *
     * @return
     */
    public String getColonSeparatedKey() {
        StringBuilder serviceNameBuilder = new StringBuilder();
        serviceNameBuilder.append(this.getServiceInterface());
        append(serviceNameBuilder, VERSION_KEY, false);
        append(serviceNameBuilder, GROUP_KEY, false);
        return serviceNameBuilder.toString();
    }

    private void append(StringBuilder target, String parameterName, boolean first) {
        String parameterValue = this.getParameter(parameterName);
        if (!isBlank(parameterValue)) {
            if (!first) {
                target.append(":");
            }
            target.append(parameterValue);
        } else {
            target.append(":");
        }
    }

    /**
     * The format of return value is '{group}/{interfaceName}:{version}'
     *
     * @return
     */
    public String getServiceKey() {
        if (serviceKey != null) {
            return serviceKey;
        }
        String inf = getServiceInterface();
        if (inf == null) {
            return null;
        }
        serviceKey = buildKey(inf, getParameter(GROUP_KEY), getParameter(VERSION_KEY));
        return serviceKey;
    }

    /**
     * The format of return value is '{group}/{path/interfaceName}:{version}'
     *
     * @return
     */
    public String getPathKey() {
        String inf = StringUtils.isNotEmpty(path) ? path : getServiceInterface();
        if (inf == null) {
            return null;
        }
        return buildKey(inf, getParameter(GROUP_KEY), getParameter(VERSION_KEY));
    }

    public static String buildKey(String path, String group, String version) {
        return BaseServiceMetadata.buildServiceKey(path, group, version);
    }

    public String getProtocolServiceKey() {
        if (protocolServiceKey != null) {
            return protocolServiceKey;
        }
        this.protocolServiceKey = getServiceKey() + ":" + getProtocol();
        return protocolServiceKey;
    }

    public String toServiceStringWithoutResolving() {
        return buildString(true, false, false, true);
    }

    public String toServiceString() {
        return buildString(true, false, true, true);
    }

    @Deprecated
    public String getServiceName() {
        return getServiceInterface();
    }

    public String getServiceInterface() {
        return getParameter(INTERFACE_KEY, path);
    }

    public URL setServiceInterface(String service) {
        return addParameter(INTERFACE_KEY, service);
    }

    /**
     * @see #getParameter(String, int)
     * @deprecated Replace to <code>getParameter(String, int)</code>
     */
    @Deprecated
    public int getIntParameter(String key) {
        return getParameter(key, 0);
    }

    /**
     * @see #getParameter(String, int)
     * @deprecated Replace to <code>getParameter(String, int)</code>
     */
    @Deprecated
    public int getIntParameter(String key, int defaultValue) {
        return getParameter(key, defaultValue);
    }

    /**
     * @see #getPositiveParameter(String, int)
     * @deprecated Replace to <code>getPositiveParameter(String, int)</code>
     */
    @Deprecated
    public int getPositiveIntParameter(String key, int defaultValue) {
        return getPositiveParameter(key, defaultValue);
    }

    /**
     * @see #getParameter(String, boolean)
     * @deprecated Replace to <code>getParameter(String, boolean)</code>
     */
    @Deprecated
    public boolean getBooleanParameter(String key) {
        return getParameter(key, false);
    }

    /**
     * @see #getParameter(String, boolean)
     * @deprecated Replace to <code>getParameter(String, boolean)</code>
     */
    @Deprecated
    public boolean getBooleanParameter(String key, boolean defaultValue) {
        return getParameter(key, defaultValue);
    }

    /**
     * @see #getMethodParameter(String, String, int)
     * @deprecated Replace to <code>getMethodParameter(String, String, int)</code>
     */
    @Deprecated
    public int getMethodIntParameter(String method, String key) {
        return getMethodParameter(method, key, 0);
    }

    /**
     * @see #getMethodParameter(String, String, int)
     * @deprecated Replace to <code>getMethodParameter(String, String, int)</code>
     */
    @Deprecated
    public int getMethodIntParameter(String method, String key, int defaultValue) {
        return getMethodParameter(method, key, defaultValue);
    }

    /**
     * @see #getMethodPositiveParameter(String, String, int)
     * @deprecated Replace to <code>getMethodPositiveParameter(String, String, int)</code>
     */
    @Deprecated
    public int getMethodPositiveIntParameter(String method, String key, int defaultValue) {
        return getMethodPositiveParameter(method, key, defaultValue);
    }

    /**
     * @see #getMethodParameter(String, String, boolean)
     * @deprecated Replace to <code>getMethodParameter(String, String, boolean)</code>
     */
    @Deprecated
    public boolean getMethodBooleanParameter(String method, String key) {
        return getMethodParameter(method, key, false);
    }

    /**
     * @see #getMethodParameter(String, String, boolean)
     * @deprecated Replace to <code>getMethodParameter(String, String, boolean)</code>
     */
    @Deprecated
    public boolean getMethodBooleanParameter(String method, String key, boolean defaultValue) {
        return getMethodParameter(method, key, defaultValue);
    }

    public Configuration toConfiguration() {
        InmemoryConfiguration configuration = new InmemoryConfiguration();
        configuration.addProperties(parameters);
        return configuration;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((parameters == null) ? 0 : parametersHashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + port;
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        URL other = (URL) obj;
        if (!StringUtils.isEquals(host, other.host)) {
            return false;
        }
        if (parameters == null) {
            if (other.parameters != null) {
                return false;
            }
        } else if (!parameters.keySet().equals(other.parameters.keySet())) {
            return false;
        } else {
            for (String key : parameters.keySet()) {
                if (key.equals(TIMESTAMP_KEY)) {
                    continue;
                }
                if (!parameters.get(key).equals(other.parameters.get(key))) {
                    return false;
                }
            }
        }

        if (!StringUtils.isEquals(password, other.password)) {
            return false;
        }
        if (!StringUtils.isEquals(path, other.path)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        if (!StringUtils.isEquals(protocol, other.protocol)) {
            return false;
        }
        if (!StringUtils.isEquals(username, other.username)) {
            return false;
        }
        return true;
    }

    private int parametersHashCode() {
        int h = 0;
        for (Map.Entry<String, String> next : parameters.entrySet()) {
            if (TIMESTAMP_KEY.equals(next.getKey())) {
                continue;
            }

            h += next.hashCode();
        }

        return h;
    }

    public static void putMethodParameter(String method, String key, String value, Map<String, Map<String, String>> methodParameters) {
        Map<String, String> subParameter = methodParameters.computeIfAbsent(method, k -> new HashMap<>());
        subParameter.put(key, value);
    }

    /* add service scope operations, see InstanceAddressURL */
    public Map<String, String> getServiceParameters(String service) {
        return getParameters();
    }

    public String getServiceParameter(String service, String key) {
        return getParameter(key);
    }

    public String getServiceParameter(String service, String key, String defaultValue) {
        String value = getServiceParameter(service, key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    public int getServiceParameter(String service, String key, int defaultValue) {
        return getParameter(key, defaultValue);
    }

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

    public float getServiceParameter(String service, String key, float defaultValue) {
        Number n = getNumbers().get(key);
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

    public long getServiceParameter(String service, String key, long defaultValue) {
        Number n = getNumbers().get(key);
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

    public short getServiceParameter(String service, String key, short defaultValue) {
        Number n = getNumbers().get(key);
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

    public byte getServiceParameter(String service, String key, byte defaultValue) {
        Number n = getNumbers().get(key);
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

    public char getServiceParameter(String service, String key, char defaultValue) {
        String value = getServiceParameter(service, key);
        return StringUtils.isEmpty(value) ? defaultValue : value.charAt(0);
    }

    public boolean getServiceParameter(String service, String key, boolean defaultValue) {
        String value = getServiceParameter(service, key);
        return StringUtils.isEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    public boolean hasServiceParameter(String service, String key) {
        String value = getServiceParameter(service, key);
        return value != null && value.length() > 0;
    }

    public float getPositiveServiceParameter(String service, String key, float defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        float value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public double getPositiveServiceParameter(String service, String key, double defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        double value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public long getPositiveServiceParameter(String service, String key, long defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        long value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public int getPositiveServiceParameter(String service, String key, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        int value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public short getPositiveServiceParameter(String service, String key, short defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        short value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public byte getPositiveServiceParameter(String service, String key, byte defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        byte value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public String getServiceMethodParameterAndDecoded(String service, String method, String key) {
        return URL.decode(getServiceMethodParameter(service, method, key));
    }

    public String getServiceMethodParameterAndDecoded(String service, String method, String key, String defaultValue) {
        return URL.decode(getServiceMethodParameter(service, method, key, defaultValue));
    }

    public String getServiceMethodParameterStrict(String service, String method, String key) {
        return getMethodParameterStrict(method, key);
    }

    public String getServiceMethodParameter(String service, String method, String key) {
        return getMethodParameter(method, key);
    }

    public String getServiceMethodParameter(String service, String method, String key, String defaultValue) {
        String value = getServiceMethodParameter(service, method, key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

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

    public short getMethodParameter(String service, String method, String key, short defaultValue) {
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

    public boolean hasServiceMethodParameter(String service, String method, String key) {
        return hasMethodParameter(method, key);
    }

    public boolean hasServiceMethodParameter(String service, String method) {
        return hasMethodParameter(method);
    }

    protected Map<String, Number> getServiceNumbers(String service) {
        return getNumbers();
    }

    protected Map<String, Map<String, Number>> getServiceMethodNumbers(String service) {
        return getMethodNumbers();
    }

}
=======
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
package org.apache.dubbo.common;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.InmemoryConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.convert.ConverterUtil;
import org.apache.dubbo.common.url.component.PathURLAddress;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.url.component.URLPlainParam;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.LRUCache;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.apache.dubbo.rpc.model.ServiceModel;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;

import static org.apache.dubbo.common.BaseServiceMetadata.COLON_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.ADDRESS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PASSWORD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.USERNAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

/**
 * URL - Uniform Resource Locator (Immutable, ThreadSafe)
 * <p>
 * url example:
 * <ul>
 * <li>http://www.facebook.com/friends?param1=value1&amp;param2=value2
 * <li>http://username:password@10.20.130.230:8080/list?version=1.0.0
 * <li>ftp://username:password@192.168.1.7:21/1/read.txt
 * <li>registry://192.168.1.7:9090/org.apache.dubbo.service1?param1=value1&amp;param2=value2
 * </ul>
 * <p>
 * Some strange example below:
 * <ul>
 * <li>192.168.1.3:20880<br>
 * for this case, url protocol = null, url host = 192.168.1.3, port = 20880, url path = null
 * <li>file:///home/user1/router.js?type=script<br>
 * for this case, url protocol = file, url host = null, url path = home/user1/router.js
 * <li>file://home/user1/router.js?type=script<br>
 * for this case, url protocol = file, url host = home, url path = user1/router.js
 * <li>file:///D:/1/router.js?type=script<br>
 * for this case, url protocol = file, url host = null, url path = D:/1/router.js
 * <li>file:/D:/1/router.js?type=script<br>
 * same as above file:///D:/1/router.js?type=script
 * <li>/home/user1/router.js?type=script <br>
 * for this case, url protocol = null, url host = null, url path = home/user1/router.js
 * <li>home/user1/router.js?type=script <br>
 * for this case, url protocol = null, url host = home, url path = user1/router.js
 * </ul>
 *
 * @see java.net.URL
 * @see java.net.URI
 */
public /*final**/
class URL implements Serializable {

    private static final long serialVersionUID = -1985165475234910535L;

    private static Map<String, URL> cachedURLs = new LRUCache<>();

    private final URLAddress urlAddress;
    private final URLParam urlParam;

    // ==== cache ====

    private transient String serviceKey;
    private transient String protocolServiceKey;
    protected volatile Map<String, Object> attributes;

    protected URL() {
        this.urlAddress = null;
        this.urlParam = URLParam.parse(new HashMap<>());
        this.attributes = null;
    }

    public URL(URLAddress urlAddress, URLParam urlParam) {
        this(urlAddress, urlParam, null);
    }

    public URL(URLAddress urlAddress, URLParam urlParam, Map<String, Object> attributes) {
        this.urlAddress = urlAddress;
        this.urlParam = null == urlParam ? URLParam.parse(new HashMap<>()) : urlParam;

        if (attributes != null && !attributes.isEmpty()) {
            this.attributes = attributes;
        } else {
            this.attributes = null;
        }
    }

    public URL(String protocol, String host, int port) {
        this(protocol, null, null, host, port, null, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, String[] pairs) { // varargs ... conflict with the following path argument, use array instead.
        this(protocol, null, null, host, port, null, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String host, int port, Map<String, String> parameters) {
        this(protocol, null, null, host, port, null, parameters);
    }

    public URL(String protocol, String host, int port, String path) {
        this(protocol, null, null, host, port, path, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, String path, String... pairs) {
        this(protocol, null, null, host, port, path, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this(protocol, null, null, host, port, path, parameters);
    }

    public URL(String protocol, String username, String password, String host, int port, String path) {
        this(protocol, username, password, host, port, path, (Map<String, String>) null);
    }

    public URL(String protocol, String username, String password, String host, int port, String path, String... pairs) {
        this(protocol, username, password, host, port, path, CollectionUtils.toStringMap(pairs));
    }

    public URL(String protocol,
               String username,
               String password,
               String host,
               int port,
               String path,
               Map<String, String> parameters) {
        if (StringUtils.isEmpty(username)
            && StringUtils.isNotEmpty(password)) {
            throw new IllegalArgumentException("Invalid url, password without username!");
        }

        this.urlAddress = new PathURLAddress(protocol, username, password, path, host, port);
        this.urlParam = URLParam.parse(parameters);
        this.attributes = null;
    }

    protected URL(String protocol,
                  String username,
                  String password,
                  String host,
                  int port,
                  String path,
                  Map<String, String> parameters,
                  boolean modifiable) {
        if (StringUtils.isEmpty(username)
            && StringUtils.isNotEmpty(password)) {
            throw new IllegalArgumentException("Invalid url, password without username!");
        }

        this.urlAddress = new PathURLAddress(protocol, username, password, path, host, port);
        this.urlParam = URLParam.parse(parameters);
        this.attributes = null;
    }

    public static URL cacheableValueOf(String url) {
        URL cachedURL = cachedURLs.get(url);
        if (cachedURL != null) {
            return cachedURL;
        }
        cachedURL = valueOf(url, false);
        cachedURLs.put(url, cachedURL);
        return cachedURL;
    }

    /**
     * parse decoded url string, formatted dubbo://host:port/path?param=value, into strutted URL.
     *
     * @param url, decoded url string
     * @return
     */
    public static URL valueOf(String url) {
        return valueOf(url, false);
    }

    public static URL valueOf(String url, ScopeModel scopeModel) {
        return valueOf(url).setScopeModel(scopeModel);
    }

    /**
     * parse normal or encoded url string into strutted URL:
     * - dubbo://host:port/path?param=value
     * - URL.encode("dubbo://host:port/path?param=value")
     *
     * @param url,     url string
     * @param encoded, encoded or decoded
     * @return
     */
    public static URL valueOf(String url, boolean encoded) {
        if (encoded) {
            return URLStrParser.parseEncodedStr(url);
        }
        return URLStrParser.parseDecodedStr(url);
    }

    public static URL valueOf(String url, String... reserveParams) {
        URL result = valueOf(url);
        if (reserveParams == null || reserveParams.length == 0) {
            return result;
        }
        Map<String, String> newMap = new HashMap<>(reserveParams.length);
        Map<String, String> oldMap = result.getParameters();
        for (String reserveParam : reserveParams) {
            String tmp = oldMap.get(reserveParam);
            if (StringUtils.isNotEmpty(tmp)) {
                newMap.put(reserveParam, tmp);
            }
        }
        return result.clearParameters().addParameters(newMap);
    }

    public static URL valueOf(URL url, String[] reserveParams, String[] reserveParamPrefixes) {
        Map<String, String> newMap = new HashMap<>();
        Map<String, String> oldMap = url.getParameters();
        if (reserveParamPrefixes != null && reserveParamPrefixes.length != 0) {
            for (Map.Entry<String, String> entry : oldMap.entrySet()) {
                for (String reserveParamPrefix : reserveParamPrefixes) {
                    if (entry.getKey().startsWith(reserveParamPrefix) && StringUtils.isNotEmpty(entry.getValue())) {
                        newMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        if (reserveParams != null) {
            for (String reserveParam : reserveParams) {
                String tmp = oldMap.get(reserveParam);
                if (StringUtils.isNotEmpty(tmp)) {
                    newMap.put(reserveParam, tmp);
                }
            }
        }
        return newMap.isEmpty() ? new ServiceConfigURL(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), url.getPath(), (Map<String, String>) null, url.getAttributes())
            : new ServiceConfigURL(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), url.getPath(), newMap, url.getAttributes());
    }

    public static String encode(String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String decode(String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    static String appendDefaultPort(String address, int defaultPort) {
        if (address != null && address.length() > 0 && defaultPort > 0) {
            int i = address.indexOf(':');
            if (i < 0) {
                return address + ":" + defaultPort;
            } else if (Integer.parseInt(address.substring(i + 1)) == 0) {
                return address.substring(0, i + 1) + defaultPort;
            }
        }
        return address;
    }

    public URLAddress getUrlAddress() {
        return urlAddress;
    }

    public URLParam getUrlParam() {
        return urlParam;
    }

    public String getProtocol() {
        return urlAddress == null ? null : urlAddress.getProtocol();
    }

    public URL setProtocol(String protocol) {
        if (urlAddress == null) {
            return new ServiceConfigURL(protocol, getHost(), getPort(), getPath(), getParameters());
        } else {
            URLAddress newURLAddress = urlAddress.setProtocol(protocol);
            return returnURL(newURLAddress);
        }
    }

    public String getUsername() {
        return urlAddress == null ? null : urlAddress.getUsername();
    }

    public URL setUsername(String username) {
        if (urlAddress == null) {
            return new ServiceConfigURL(getProtocol(), getHost(), getPort(), getPath(), getParameters()).setUsername(username);
        } else {
            URLAddress newURLAddress = urlAddress.setUsername(username);
            return returnURL(newURLAddress);
        }
    }

    public String getPassword() {
        return urlAddress == null ? null : urlAddress.getPassword();
    }

    public URL setPassword(String password) {
        if (urlAddress == null) {
            return new ServiceConfigURL(getProtocol(), getHost(), getPort(), getPath(), getParameters()).setPassword(password);
        } else {
            URLAddress newURLAddress = urlAddress.setPassword(password);
            return returnURL(newURLAddress);
        }
    }

    /**
     * refer to https://datatracker.ietf.org/doc/html/rfc3986
     *
     * @return authority
     */
    public String getAuthority() {
        StringBuilder ret = new StringBuilder();

        ret.append(getUserInformation());

        if (StringUtils.isNotEmpty(getHost())) {
            if (StringUtils.isNotEmpty(getUsername()) || StringUtils.isNotEmpty(getPassword())) {
                ret.append('@');
            }
            ret.append(getHost());
            if (getPort() != 0) {
                ret.append(':');
                ret.append(getPort());
            }
        }

        return ret.length() == 0 ? null : ret.toString();
    }

    /**
     * refer to https://datatracker.ietf.org/doc/html/rfc3986
     *
     * @return user information
     */
    public String getUserInformation() {
        StringBuilder ret = new StringBuilder();

        if (StringUtils.isEmpty(getUsername()) && StringUtils.isEmpty(getPassword())) {
            return ret.toString();
        }

        if (StringUtils.isNotEmpty(getUsername())) {
            ret.append(getUsername());
        }

        ret.append(':');

        if (StringUtils.isNotEmpty(getPassword())) {
            ret.append(getPassword());
        }

        return ret.length() == 0 ? null : ret.toString();
    }

    public String getHost() {
        return urlAddress == null ? null : urlAddress.getHost();
    }

    public URL setHost(String host) {
        if (urlAddress == null) {
            return new ServiceConfigURL(getProtocol(), host, getPort(), getPath(), getParameters());
        } else {
            URLAddress newURLAddress = urlAddress.setHost(host);
            return returnURL(newURLAddress);
        }
    }


    public int getPort() {
        return urlAddress == null ? 0 : urlAddress.getPort();
    }

    public URL setPort(int port) {
        if (urlAddress == null) {
            return new ServiceConfigURL(getProtocol(), getHost(), port, getPath(), getParameters());
        } else {
            URLAddress newURLAddress = urlAddress.setPort(port);
            return returnURL(newURLAddress);
        }
    }

    public int getPort(int defaultPort) {
        int port = getPort();
        return port <= 0 ? defaultPort : port;
    }

    public String getAddress() {
        return urlAddress == null ? null : urlAddress.getAddress();
    }

    public URL setAddress(String address) {
        int i = address.lastIndexOf(':');
        String host;
        int port = this.getPort();
        if (i >= 0) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
        }
        if (urlAddress == null) {
            return new ServiceConfigURL(getProtocol(), host, port, getPath(), getParameters());
        } else {
            URLAddress newURLAddress = urlAddress.setAddress(host, port);
            return returnURL(newURLAddress);
        }
    }

    public String getIp() {
        return urlAddress == null ? null : urlAddress.getIp();
    }

    public String getBackupAddress() {
        return getBackupAddress(0);
    }

    public String getBackupAddress(int defaultPort) {
        StringBuilder address = new StringBuilder(appendDefaultPort(getAddress(), defaultPort));
        String[] backups = getParameter(RemotingConstants.BACKUP_KEY, new String[0]);
        if (ArrayUtils.isNotEmpty(backups)) {
            for (String backup : backups) {
                address.append(',');
                address.append(appendDefaultPort(backup, defaultPort));
            }
        }
        return address.toString();
    }

    public List<URL> getBackupUrls() {
        List<URL> urls = new ArrayList<>();
        urls.add(this);
        String[] backups = getParameter(RemotingConstants.BACKUP_KEY, new String[0]);
        if (backups != null && backups.length > 0) {
            for (String backup : backups) {
                urls.add(this.setAddress(backup));
            }
        }
        return urls;
    }

    public String getPath() {
        return urlAddress == null ? null : urlAddress.getPath();
    }

    public URL setPath(String path) {
        if (urlAddress == null) {
            return new ServiceConfigURL(getProtocol(), getHost(), getPort(), path, getParameters());
        } else {
            URLAddress newURLAddress = urlAddress.setPath(path);
            return returnURL(newURLAddress);
        }
    }

    public String getAbsolutePath() {
        String path = getPath();
        if (path != null && !path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

    public Map<String, String> getOriginalParameters() {
        return this.getParameters();
    }

    public Map<String, String> getParameters() {
        return urlParam.getParameters();
    }

    public Map<String, String> getAllParameters() {
        return this.getParameters();
    }

    /**
     * Get the parameters to be selected(filtered)
     *
     * @param nameToSelect the {@link Predicate} to select the parameter name
     * @return non-null {@link Map}
     * @since 2.7.8
     */
    public Map<String, String> getParameters(Predicate<String> nameToSelect) {
        Map<String, String> selectedParameters = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : getParameters().entrySet()) {
            String name = entry.getKey();
            if (nameToSelect.test(name)) {
                selectedParameters.put(name, entry.getValue());
            }
        }
        return Collections.unmodifiableMap(selectedParameters);
    }

    public String getParameterAndDecoded(String key) {
        return getParameterAndDecoded(key, null);
    }

    public String getParameterAndDecoded(String key, String defaultValue) {
        return decode(getParameter(key, defaultValue));
    }

    public String getOriginalParameter(String key) {
        return getParameter(key);
    }

    public String getParameter(String key) {
        return urlParam.getParameter(key);
    }

    public String getParameter(String key, String defaultValue) {
        String value = getParameter(key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    public String[] getParameter(String key, String[] defaultValue) {
        String value = getParameter(key);
        return StringUtils.isEmpty(value) ? defaultValue : COMMA_SPLIT_PATTERN.split(value);
    }

    public List<String> getParameter(String key, List<String> defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        String[] strArray = COMMA_SPLIT_PATTERN.split(value);
        return Arrays.asList(strArray);
    }

    /**
     * Get parameter
     *
     * @param key       the key of parameter
     * @param valueType the type of parameter value
     * @param <T>       the type of parameter value
     * @return get the parameter if present, or <code>null</code>
     * @since 2.7.8
     */
    public <T> T getParameter(String key, Class<T> valueType) {
        return getParameter(key, valueType, null);
    }

    /**
     * Get parameter
     *
     * @param key          the key of parameter
     * @param valueType    the type of parameter value
     * @param defaultValue the default value if parameter is absent
     * @param <T>          the type of parameter value
     * @return get the parameter if present, or <code>defaultValue</code> will be used.
     * @since 2.7.8
     */
    public <T> T getParameter(String key, Class<T> valueType, T defaultValue) {
        String value = getParameter(key);
        T result = null;
        if (!isBlank(value)) {
            result = getOrDefaultFrameworkModel().getBeanFactory().getBean(ConverterUtil.class).convertIfPossible(value, valueType);
        }
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    public URL setScopeModel(ScopeModel scopeModel) {
        return putAttribute(CommonConstants.SCOPE_MODEL, scopeModel);
    }

    public ScopeModel getScopeModel() {
        return (ScopeModel) getAttribute(CommonConstants.SCOPE_MODEL);
    }

    public FrameworkModel getOrDefaultFrameworkModel() {
        return ScopeModelUtil.getFrameworkModel(getScopeModel());
    }

    public ApplicationModel getOrDefaultApplicationModel() {
        return ScopeModelUtil.getApplicationModel(getScopeModel());
    }

    public ApplicationModel getApplicationModel() {
        return ScopeModelUtil.getOrNullApplicationModel(getScopeModel());
    }

    public ModuleModel getOrDefaultModuleModel() {
        return ScopeModelUtil.getModuleModel(getScopeModel());
    }

    public URL setServiceModel(ServiceModel serviceModel) {
        return putAttribute(CommonConstants.SERVICE_MODEL, serviceModel);
    }

    public ServiceModel getServiceModel() {
        return (ServiceModel) getAttribute(CommonConstants.SERVICE_MODEL);
    }



    public URL getUrlParameter(String key) {
        String value = getParameterAndDecoded(key);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return URL.valueOf(value);
    }

    public double getParameter(String key, double defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    public float getParameter(String key, float defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Float.parseFloat(value);
    }

    public long getParameter(String key, long defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    public int getParameter(String key, int defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public short getParameter(String key, short defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Short.parseShort(value);
    }

    public byte getParameter(String key, byte defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Byte.parseByte(value);
    }

    public float getPositiveParameter(String key, float defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        float value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public double getPositiveParameter(String key, double defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        double value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public long getPositiveParameter(String key, long defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        long value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public int getPositiveParameter(String key, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        int value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public short getPositiveParameter(String key, short defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        short value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public byte getPositiveParameter(String key, byte defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        byte value = getParameter(key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public char getParameter(String key, char defaultValue) {
        String value = getParameter(key);
        return StringUtils.isEmpty(value) ? defaultValue : value.charAt(0);
    }

    public boolean getParameter(String key, boolean defaultValue) {
        String value = getParameter(key);
        return StringUtils.isEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    public boolean hasParameter(String key) {
        String value = getParameter(key);
        return value != null && value.length() > 0;
    }

    public String getMethodParameterAndDecoded(String method, String key) {
        return URL.decode(getMethodParameter(method, key));
    }

    public String getMethodParameterAndDecoded(String method, String key, String defaultValue) {
        return URL.decode(getMethodParameter(method, key, defaultValue));
    }

    public String getMethodParameter(String method, String key) {
        return urlParam.getMethodParameter(method, key);
    }

    public String getMethodParameterStrict(String method, String key) {
        return urlParam.getMethodParameterStrict(method, key);
    }

    public String getMethodParameter(String method, String key, String defaultValue) {
        String value = getMethodParameter(method, key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    public double getMethodParameter(String method, String key, double defaultValue) {
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    public float getMethodParameter(String method, String key, float defaultValue) {
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Float.parseFloat(value);
    }

    public long getMethodParameter(String method, String key, long defaultValue) {
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    public int getMethodParameter(String method, String key, int defaultValue) {
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public short getMethodParameter(String method, String key, short defaultValue) {
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Short.parseShort(value);
    }

    public byte getMethodParameter(String method, String key, byte defaultValue) {
        String value = getMethodParameter(method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Byte.parseByte(value);
    }

    public double getMethodPositiveParameter(String method, String key, double defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        double value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public float getMethodPositiveParameter(String method, String key, float defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        float value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public long getMethodPositiveParameter(String method, String key, long defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        long value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public int getMethodPositiveParameter(String method, String key, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        int value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public short getMethodPositiveParameter(String method, String key, short defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        short value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public byte getMethodPositiveParameter(String method, String key, byte defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        byte value = getMethodParameter(method, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public char getMethodParameter(String method, String key, char defaultValue) {
        String value = getMethodParameter(method, key);
        return StringUtils.isEmpty(value) ? defaultValue : value.charAt(0);
    }

    public boolean getMethodParameter(String method, String key, boolean defaultValue) {
        String value = getMethodParameter(method, key);
        return StringUtils.isEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    public boolean hasMethodParameter(String method, String key) {
        if (method == null) {
            String suffix = "." + key;
            for (String fullKey : getParameters().keySet()) {
                if (fullKey.endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        }
        if (key == null) {
            String prefix = method + ".";
            for (String fullKey : getParameters().keySet()) {
                if (fullKey.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
        String value = getMethodParameterStrict(method, key);
        return StringUtils.isNotEmpty(value);
    }

    public String getAnyMethodParameter(String key) {
        return urlParam.getAnyMethodParameter(key);
    }

    public boolean hasMethodParameter(String method) {
        return urlParam.hasMethodParameter(method);
    }

    public boolean isLocalHost() {
        return NetUtils.isLocalHost(getHost()) || getParameter(LOCALHOST_KEY, false);
    }

    public boolean isAnyHost() {
        return ANYHOST_VALUE.equals(getHost()) || getParameter(ANYHOST_KEY, false);
    }

    public URL addParameterAndEncoded(String key, String value) {
        if (StringUtils.isEmpty(value)) {
            return this;
        }
        return addParameter(key, encode(value));
    }

    public URL addParameter(String key, boolean value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, char value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, byte value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, short value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, int value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, long value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, float value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, double value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, Enum<?> value) {
        if (value == null) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, Number value) {
        if (value == null) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, CharSequence value) {
        if (value == null || value.length() == 0) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, String value) {
        URLParam newParam = urlParam.addParameter(key, value);
        return returnURL(newParam);
    }

    public URL addParameterIfAbsent(String key, String value) {
        URLParam newParam = urlParam.addParameterIfAbsent(key, value);
        return returnURL(newParam);
    }

    /**
     * Add parameters to a new url.
     *
     * @param parameters parameters in key-value pairs
     * @return A new URL
     */
    public URL addParameters(Map<String, String> parameters) {
        URLParam newParam = urlParam.addParameters(parameters);
        return returnURL(newParam);
    }

    public URL addParametersIfAbsent(Map<String, String> parameters) {
        URLParam newURLParam = urlParam.addParametersIfAbsent(parameters);
        return returnURL(newURLParam);
    }

    public URL addParameters(String... pairs) {
        if (pairs == null || pairs.length == 0) {
            return this;
        }
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Map pairs can not be odd number.");
        }
        Map<String, String> map = new HashMap<>();
        int len = pairs.length / 2;
        for (int i = 0; i < len; i++) {
            map.put(pairs[2 * i], pairs[2 * i + 1]);
        }
        return addParameters(map);
    }

    public URL addParameterString(String query) {
        if (StringUtils.isEmpty(query)) {
            return this;
        }
        return addParameters(StringUtils.parseQueryString(query));
    }

    public URL removeParameter(String key) {
        if (StringUtils.isEmpty(key)) {
            return this;
        }
        return removeParameters(key);
    }

    public URL removeParameters(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return this;
        }
        return removeParameters(keys.toArray(new String[0]));
    }

    public URL removeParameters(String... keys) {
        URLParam newURLParam = urlParam.removeParameters(keys);
        return returnURL(newURLParam);
    }

    public URL clearParameters() {
        URLParam newURLParam = urlParam.clearParameters();
        return returnURL(newURLParam);
    }

    public String getRawParameter(String key) {
        if (PROTOCOL_KEY.equals(key)) {
            return urlAddress.getProtocol();
        }
        if (USERNAME_KEY.equals(key)) {
            return urlAddress.getUsername();
        }
        if (PASSWORD_KEY.equals(key)) {
            return urlAddress.getPassword();
        }
        if (HOST_KEY.equals(key)) {
            return urlAddress.getHost();
        }
        if (PORT_KEY.equals(key)) {
            return String.valueOf(urlAddress.getPort());
        }
        if (PATH_KEY.equals(key)) {
            return urlAddress.getPath();
        }
        return urlParam.getParameter(key);
    }

    public Map<String, String> toOriginalMap() {
        Map<String, String> map = new HashMap<>(getOriginalParameters());
        return addSpecialKeys(map);
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>(getParameters());
        return addSpecialKeys(map);
    }

    private Map<String, String> addSpecialKeys(Map<String, String> map) {
        if (getProtocol() != null) {
            map.put(PROTOCOL_KEY, getProtocol());
        }
        if (getUsername() != null) {
            map.put(USERNAME_KEY, getUsername());
        }
        if (getPassword() != null) {
            map.put(PASSWORD_KEY, getPassword());
        }
        if (getHost() != null) {
            map.put(HOST_KEY, getHost());
        }
        if (getPort() > 0) {
            map.put(PORT_KEY, String.valueOf(getPort()));
        }
        if (getPath() != null) {
            map.put(PATH_KEY, getPath());
        }
        if (getAddress() != null) {
            map.put(ADDRESS_KEY, getAddress());
        }
        return map;
    }

    @Override
    public String toString() {
        return buildString(false, true); // no show username and password
    }

    public String toString(String... parameters) {
        return buildString(false, true, parameters); // no show username and password
    }

    public String toIdentityString() {
        return buildString(true, false); // only return identity message, see the method "equals" and "hashCode"
    }

    public String toIdentityString(String... parameters) {
        return buildString(true, false, parameters); // only return identity message, see the method "equals" and "hashCode"
    }

    public String toFullString() {
        return buildString(true, true);
    }

    public String toFullString(String... parameters) {
        return buildString(true, true, parameters);
    }

    public String toParameterString() {
        return toParameterString(new String[0]);
    }

    public String toParameterString(String... parameters) {
        StringBuilder buf = new StringBuilder();
        buildParameters(buf, false, parameters);
        return buf.toString();
    }

    protected void buildParameters(StringBuilder buf, boolean concat, String[] parameters) {
        if (CollectionUtils.isNotEmptyMap(getParameters())) {
            List<String> includes = (ArrayUtils.isEmpty(parameters) ? null : Arrays.asList(parameters));
            boolean first = true;
            for (Map.Entry<String, String> entry : new TreeMap<>(getParameters()).entrySet()) {
                if (StringUtils.isNotEmpty(entry.getKey())
                    && (includes == null || includes.contains(entry.getKey()))) {
                    if (first) {
                        if (concat) {
                            buf.append('?');
                        }
                        first = false;
                    } else {
                        buf.append('&');
                    }
                    buf.append(entry.getKey());
                    buf.append('=');
                    buf.append(entry.getValue() == null ? "" : entry.getValue().trim());
                }
            }
        }
    }

    private String buildString(boolean appendUser, boolean appendParameter, String... parameters) {
        return buildString(appendUser, appendParameter, false, false, parameters);
    }

    private String buildString(boolean appendUser, boolean appendParameter, boolean useIP, boolean useService, String... parameters) {
        StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotEmpty(getProtocol())) {
            buf.append(getProtocol());
            buf.append("://");
        }
        if (appendUser && StringUtils.isNotEmpty(getUsername())) {
            buf.append(getUsername());
            if (StringUtils.isNotEmpty(getPassword())) {
                buf.append(':');
                buf.append(getPassword());
            }
            buf.append('@');
        }
        String host;
        if (useIP) {
            host = urlAddress.getIp();
        } else {
            host = getHost();
        }
        if (StringUtils.isNotEmpty(host)) {
            buf.append(host);
            if (getPort() > 0) {
                buf.append(':');
                buf.append(getPort());
            }
        }
        String path;
        if (useService) {
            path = getServiceKey();
        } else {
            path = getPath();
        }
        if (StringUtils.isNotEmpty(path)) {
            buf.append('/');
            buf.append(path);
        }

        if (appendParameter) {
            buildParameters(buf, true, parameters);
        }
        return buf.toString();
    }

    public java.net.URL toJavaURL() {
        try {
            return new java.net.URL(toString());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public InetSocketAddress toInetSocketAddress() {
        return new InetSocketAddress(getHost(), getPort());
    }

    /**
     * The format is "{interface}:[version]:[group]"
     *
     * @return
     */
    public String getColonSeparatedKey() {
        StringBuilder serviceNameBuilder = new StringBuilder();
        serviceNameBuilder.append(this.getServiceInterface());
        append(serviceNameBuilder, VERSION_KEY, false);
        append(serviceNameBuilder, GROUP_KEY, false);
        return serviceNameBuilder.toString();
    }

    private void append(StringBuilder target, String parameterName, boolean first) {
        String parameterValue = this.getParameter(parameterName);
        if (!isBlank(parameterValue)) {
            if (!first) {
                target.append(':');
            }
            target.append(parameterValue);
        } else {
            target.append(':');
        }
    }

    /**
     * The format of return value is '{group}/{interfaceName}:{version}'
     *
     * @return
     */
    public String getServiceKey() {
        if (serviceKey != null) {
            return serviceKey;
        }
        String inf = getServiceInterface();
        if (inf == null) {
            return null;
        }
        serviceKey = buildKey(inf, getGroup(), getVersion());
        return serviceKey;
    }

    /**
     * Format : interface:version
     *
     * @return
     */
    public String getDisplayServiceKey() {
        if (StringUtils.isEmpty(getVersion())) {
            return getServiceInterface();
        }
        return getServiceInterface() +
            COLON_SEPARATOR + getVersion();
    }

    /**
     * The format of return value is '{group}/{path/interfaceName}:{version}'
     *
     * @return
     */
    public String getPathKey() {
        String inf = StringUtils.isNotEmpty(getPath()) ? getPath() : getServiceInterface();
        if (inf == null) {
            return null;
        }
        return buildKey(inf, getGroup(), getVersion());
    }

    public static String buildKey(String path, String group, String version) {
        return BaseServiceMetadata.buildServiceKey(path, group, version);
    }

    public String getProtocolServiceKey() {
        if (protocolServiceKey != null) {
            return protocolServiceKey;
        }
        this.protocolServiceKey = getServiceKey();
        /*
        Special treatment for urls begins with 'consumer://', that is, a consumer subscription url instance with no protocol specified.
        If protocol is specified on the consumer side, then this method will return as normal.
        */
        if (!CONSUMER.equals(getProtocol())) {
            this.protocolServiceKey += (GROUP_CHAR_SEPARATOR + getProtocol());
        }
        return protocolServiceKey;
    }

    public String toServiceStringWithoutResolving() {
        return buildString(true, false, false, true);
    }

    public String toServiceString() {
        return buildString(true, false, true, true);
    }

    @Deprecated
    public String getServiceName() {
        return getServiceInterface();
    }

    public String getServiceInterface() {
        return getParameter(INTERFACE_KEY, getPath());
    }

    public URL setServiceInterface(String service) {
        return addParameter(INTERFACE_KEY, service);
    }

    /**
     * @see #getParameter(String, int)
     * @deprecated Replace to <code>getParameter(String, int)</code>
     */
    @Deprecated
    public int getIntParameter(String key) {
        return getParameter(key, 0);
    }

    /**
     * @see #getParameter(String, int)
     * @deprecated Replace to <code>getParameter(String, int)</code>
     */
    @Deprecated
    public int getIntParameter(String key, int defaultValue) {
        return getParameter(key, defaultValue);
    }

    /**
     * @see #getPositiveParameter(String, int)
     * @deprecated Replace to <code>getPositiveParameter(String, int)</code>
     */
    @Deprecated
    public int getPositiveIntParameter(String key, int defaultValue) {
        return getPositiveParameter(key, defaultValue);
    }

    /**
     * @see #getParameter(String, boolean)
     * @deprecated Replace to <code>getParameter(String, boolean)</code>
     */
    @Deprecated
    public boolean getBooleanParameter(String key) {
        return getParameter(key, false);
    }

    /**
     * @see #getParameter(String, boolean)
     * @deprecated Replace to <code>getParameter(String, boolean)</code>
     */
    @Deprecated
    public boolean getBooleanParameter(String key, boolean defaultValue) {
        return getParameter(key, defaultValue);
    }

    /**
     * @see #getMethodParameter(String, String, int)
     * @deprecated Replace to <code>getMethodParameter(String, String, int)</code>
     */
    @Deprecated
    public int getMethodIntParameter(String method, String key) {
        return getMethodParameter(method, key, 0);
    }

    /**
     * @see #getMethodParameter(String, String, int)
     * @deprecated Replace to <code>getMethodParameter(String, String, int)</code>
     */
    @Deprecated
    public int getMethodIntParameter(String method, String key, int defaultValue) {
        return getMethodParameter(method, key, defaultValue);
    }

    /**
     * @see #getMethodPositiveParameter(String, String, int)
     * @deprecated Replace to <code>getMethodPositiveParameter(String, String, int)</code>
     */
    @Deprecated
    public int getMethodPositiveIntParameter(String method, String key, int defaultValue) {
        return getMethodPositiveParameter(method, key, defaultValue);
    }

    /**
     * @see #getMethodParameter(String, String, boolean)
     * @deprecated Replace to <code>getMethodParameter(String, String, boolean)</code>
     */
    @Deprecated
    public boolean getMethodBooleanParameter(String method, String key) {
        return getMethodParameter(method, key, false);
    }

    /**
     * @see #getMethodParameter(String, String, boolean)
     * @deprecated Replace to <code>getMethodParameter(String, String, boolean)</code>
     */
    @Deprecated
    public boolean getMethodBooleanParameter(String method, String key, boolean defaultValue) {
        return getMethodParameter(method, key, defaultValue);
    }

    public Configuration toConfiguration() {
        InmemoryConfiguration configuration = new InmemoryConfiguration();
        configuration.addProperties(getParameters());
        return configuration;
    }

    private volatile int hashCodeCache = -1;

    @Override
    public int hashCode() {
        if (hashCodeCache == -1) {
            hashCodeCache = Objects.hash(urlAddress, urlParam);
        }
        return hashCodeCache;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof URL)) {
            return false;
        }
        URL other = (URL) obj;
        return Objects.equals(this.getUrlAddress(), other.getUrlAddress()) && Objects.equals(this.getUrlParam(), other.getUrlParam());
    }

    public static void putMethodParameter(String method, String key, String value, Map<String, Map<String, String>> methodParameters) {
        Map<String, String> subParameter = methodParameters.computeIfAbsent(method, k -> new HashMap<>());
        subParameter.put(key, value);
    }

    protected <T extends URL> T newURL(URLAddress urlAddress, URLParam urlParam) {
        return (T) new ServiceConfigURL(urlAddress, urlParam, attributes);
    }

    /* methods introduced for CompositeURL, CompositeURL must override to make the implementations meaningful */

    public String getApplication(String defaultValue) {
        String value = getApplication();
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    public String getApplication() {
        return getParameter(APPLICATION_KEY);
    }

    public String getRemoteApplication() {
        return getParameter(REMOTE_APPLICATION_KEY);
    }

    public String getGroup() {
        return getParameter(GROUP_KEY);
    }

    public String getGroup(String defaultValue) {
        String value = getGroup();
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    public String getVersion() {
        return getParameter(VERSION_KEY);
    }

    public String getVersion(String defaultValue) {
        String value = getVersion();
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    public String getConcatenatedParameter(String key) {
        return getParameter(key);
    }

    public String getCategory(String defaultValue) {
        String value = getCategory();
        if (StringUtils.isEmpty(value)) {
            value = defaultValue;
        }
        return value;
    }

    public String[] getCategory(String[] defaultValue) {
        String value = getCategory();
        return StringUtils.isEmpty(value) ? defaultValue : COMMA_SPLIT_PATTERN.split(value);
    }

    public String getCategory() {
        return getParameter(CATEGORY_KEY);
    }

    public String getSide(String defaultValue) {
        String value = getSide();
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    public String getSide() {
        return getParameter(SIDE_KEY);
    }

    /* Service Config URL, START*/
    public Map<String, Object> getAttributes() {
        return attributes == null ? Collections.emptyMap() : attributes;
    }

    public URL addAttributes(Map<String, Object> attributes) {
        if (attributes != null) {
            attributes.putAll(attributes);
        }
        return this;
    }

    public Object getAttribute(String key) {
        return attributes == null ? null : attributes.get(key);
    }

    public Object getAttribute(String key, Object defaultValue) {
        Object val = attributes == null ? null : attributes.get(key);
        return val != null ? val : defaultValue;
    }

    public URL putAttribute(String key, Object obj) {
        if (attributes == null) {
            this.attributes = new HashMap<>();
        }
        attributes.put(key, obj);
        return this;
    }

    public URL removeAttribute(String key) {
        if (attributes != null) {
            attributes.remove(key);
        }
        return this;
    }

    public boolean hasAttribute(String key) {
        return attributes != null && attributes.containsKey(key);
    }

    /* Service Config URL, END*/

    private URL returnURL(URLAddress newURLAddress) {
        if (urlAddress == newURLAddress) {
            return this;
        }
        return newURL(newURLAddress, urlParam);
    }

    private URL returnURL(URLParam newURLParam) {
        if (urlParam == newURLParam) {
            return this;
        }
        return newURL(urlAddress, newURLParam);
    }

    /* add service scope operations, see InstanceAddressURL */
    public Map<String, String> getOriginalServiceParameters(String service) {
        return getServiceParameters(service);
    }

    public Map<String, String> getServiceParameters(String service) {
        return getParameters();
    }

    public String getOriginalServiceParameter(String service, String key) {
        return this.getServiceParameter(service, key);
    }

    public String getServiceParameter(String service, String key) {
        return getParameter(key);
    }

    public String getServiceParameter(String service, String key, String defaultValue) {
        String value = getServiceParameter(service, key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    public int getServiceParameter(String service, String key, int defaultValue) {
        return getParameter(key, defaultValue);
    }

    public double getServiceParameter(String service, String key, double defaultValue) {
        String value = getServiceParameter(service, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    public float getServiceParameter(String service, String key, float defaultValue) {
        String value = getServiceParameter(service, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Float.parseFloat(value);
    }

    public long getServiceParameter(String service, String key, long defaultValue) {
        String value = getServiceParameter(service, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    public short getServiceParameter(String service, String key, short defaultValue) {
        String value = getServiceParameter(service, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Short.parseShort(value);
    }

    public byte getServiceParameter(String service, String key, byte defaultValue) {
        String value = getServiceParameter(service, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Byte.parseByte(value);
    }

    public char getServiceParameter(String service, String key, char defaultValue) {
        String value = getServiceParameter(service, key);
        return StringUtils.isEmpty(value) ? defaultValue : value.charAt(0);
    }

    public boolean getServiceParameter(String service, String key, boolean defaultValue) {
        String value = getServiceParameter(service, key);
        return StringUtils.isEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    public boolean hasServiceParameter(String service, String key) {
        String value = getServiceParameter(service, key);
        return value != null && value.length() > 0;
    }

    public float getPositiveServiceParameter(String service, String key, float defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        float value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public double getPositiveServiceParameter(String service, String key, double defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        double value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public long getPositiveServiceParameter(String service, String key, long defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        long value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public int getPositiveServiceParameter(String service, String key, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        int value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public short getPositiveServiceParameter(String service, String key, short defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        short value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public byte getPositiveServiceParameter(String service, String key, byte defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        byte value = getServiceParameter(service, key, defaultValue);
        return value <= 0 ? defaultValue : value;
    }

    public String getServiceMethodParameterAndDecoded(String service, String method, String key) {
        return URL.decode(getServiceMethodParameter(service, method, key));
    }

    public String getServiceMethodParameterAndDecoded(String service, String method, String key, String defaultValue) {
        return URL.decode(getServiceMethodParameter(service, method, key, defaultValue));
    }

    public String getServiceMethodParameterStrict(String service, String method, String key) {
        return getMethodParameterStrict(method, key);
    }

    public String getServiceMethodParameter(String service, String method, String key) {
        return getMethodParameter(method, key);
    }

    public String getServiceMethodParameter(String service, String method, String key, String defaultValue) {
        String value = getServiceMethodParameter(service, method, key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    public double getServiceMethodParameter(String service, String method, String key, double defaultValue) {
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    public float getServiceMethodParameter(String service, String method, String key, float defaultValue) {
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Float.parseFloat(value);
    }

    public long getServiceMethodParameter(String service, String method, String key, long defaultValue) {
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    public int getServiceMethodParameter(String service, String method, String key, int defaultValue) {
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public short getServiceMethodParameter(String service, String method, String key, short defaultValue) {
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Short.parseShort(value);
    }

    public byte getServiceMethodParameter(String service, String method, String key, byte defaultValue) {
        String value = getServiceMethodParameter(service, method, key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return Byte.parseByte(value);
    }

    public boolean hasServiceMethodParameter(String service, String method, String key) {
        return hasMethodParameter(method, key);
    }

    public boolean hasServiceMethodParameter(String service, String method) {
        return hasMethodParameter(method);
    }

    public URL toSerializableURL() {
        return returnURL(URLPlainParam.toURLPlainParam(urlParam));
    }
}
>>>>>>> origin/3.2
