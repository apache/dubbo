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

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;

public class URLAddress implements Serializable {
    private static final long serialVersionUID = -1985165475234910535L;

    protected String host;
    protected int port;

    // cache
    protected transient String rawAddress;
    protected transient long timestamp;

    public URLAddress(String host, int port) {
        this(host, port, null);
    }

    public URLAddress(String host, int port, String rawAddress) {
        this.host = host;
        port = Math.max(port, 0);
        this.port = port;

        this.rawAddress = rawAddress;
        this.timestamp = System.currentTimeMillis();
    }

    public String getProtocol() {
        return "";
    }

    public URLAddress setProtocol(String protocol) {
        return this;
    }

    public String getUsername() {
        return "";
    }

    public URLAddress setUsername(String username) {
        return this;
    }

    public String getPassword() {
        return "";
    }

    public URLAddress setPassword(String password) {
        return this;
    }

    public String getPath() {
        return "";
    }

    public URLAddress setPath(String path) {
        return this;
    }

    public String getHost() {
        return host;
    }

    public URLAddress setHost(String host) {
        return new URLAddress(host, port, rawAddress);
    }

    public int getPort() {
        return port;
    }

    public URLAddress setPort(int port) {
        return new URLAddress(host, port, rawAddress);
    }

    public String getAddress() {
        if (rawAddress == null) {
            rawAddress = getAddress(getHost(), getPort());
        }
        return rawAddress;
    }

    public URLAddress setAddress(String host, int port) {
        return new URLAddress(host, port, rawAddress);
    }

    public String getIp() {
        return NetUtils.getIpByHost(getHost());
    }

    public String getRawAddress() {
        return rawAddress;
    }

    protected String getAddress(String host, int port) {
        return port <= 0 ? host : host + ':' + port;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int hashCode() {
        return host.hashCode() * 31 + port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof URLAddress)) return false;
        URLAddress that = (URLAddress) obj;
        return Objects.equals(this.getProtocol(), that.getProtocol()) &&
                Objects.equals(this.getUsername(), that.getUsername()) &&
                Objects.equals(this.getPassword(), that.getPassword()) &&
                Objects.equals(this.getPath(), that.getPath()) &&
                Objects.equals(this.getHost(), that.getHost()) &&
                Objects.equals(this.getPort(), that.getPort());
    }

    @Override
    public String toString() {
        if (rawAddress != null) {
            return rawAddress;
        }

        StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotEmpty(host)) {
            buf.append(host);
            if (port > 0) {
                buf.append(':');
                buf.append(port);
            }
        }
        return buf.toString();
    }

    public static URLAddress parse(String rawAddress, String defaultProtocol, boolean encoded) {
        try {
            String decodeStr = rawAddress;
            if (encoded) {
                decodeStr = URLDecoder.decode(rawAddress, "UTF-8");
            }

            boolean isPathAddress = decodeStr.contains(PATH_SEPARATOR);
            if (isPathAddress) {
                return createPathURLAddress(decodeStr, rawAddress, defaultProtocol);
            }
            return createURLAddress(decodeStr, rawAddress, defaultProtocol);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static URLAddress createURLAddress(String decodeStr, String rawAddress, String defaultProtocol) {
        String host = null;
        int port = 0;

        int i = decodeStr.lastIndexOf(':');
        if (i >= 0 && i < decodeStr.length() - 1) {
            if (decodeStr.lastIndexOf('%') > i) {
                // ipv6 address with scope id
                // e.g. fe80:0:0:0:894:aeec:f37d:23e1%en0
                // see https://howdoesinternetwork.com/2013/ipv6-zone-id
                // ignore
            } else {
                port = Integer.parseInt(decodeStr.substring(i + 1));
                host = decodeStr.substring(0, i);
            }
        } else {
            host = decodeStr;
        }

        return new URLAddress(host, port, rawAddress);
    }

    private static PathURLAddress createPathURLAddress(String decodeStr, String rawAddress, String defaultProtocol) {
        String protocol = defaultProtocol;
        String path = null, username = null, password = null, host = null;
        int port = 0;
        int i = decodeStr.indexOf("://");
        if (i >= 0) {
            if (i == 0) {
                throw new IllegalStateException("url missing protocol: \"" + decodeStr + "\"");
            }
            protocol = decodeStr.substring(0, i);
            decodeStr = decodeStr.substring(i + 3);
        } else {
            // case: file:/path/to/file.txt
            i = decodeStr.indexOf(":/");
            if (i >= 0) {
                if (i == 0) {
                    throw new IllegalStateException("url missing protocol: \"" + decodeStr + "\"");
                }
                protocol = decodeStr.substring(0, i);
                decodeStr = decodeStr.substring(i + 1);
            }
        }

        i = decodeStr.indexOf('/');
        if (i >= 0) {
            path = decodeStr.substring(i + 1);
            decodeStr = decodeStr.substring(0, i);
        }
        i = decodeStr.lastIndexOf('@');
        if (i >= 0) {
            username = decodeStr.substring(0, i);
            int j = username.indexOf(':');
            if (j >= 0) {
                password = username.substring(j + 1);
                username = username.substring(0, j);
            }
            decodeStr = decodeStr.substring(i + 1);
        }
        i = decodeStr.lastIndexOf(':');
        if (i >= 0 && i < decodeStr.length() - 1) {
            if (decodeStr.lastIndexOf('%') > i) {
                // ipv6 address with scope id
                // e.g. fe80:0:0:0:894:aeec:f37d:23e1%en0
                // see https://howdoesinternetwork.com/2013/ipv6-zone-id
                // ignore
            } else {
                port = Integer.parseInt(decodeStr.substring(i + 1));
                host = decodeStr.substring(0, i);
            }
        }

        // check cache
        protocol = URLItemCache.intern(protocol);
        path = URLItemCache.checkPath(path);

        return new PathURLAddress(protocol, username, password, path, host, port, rawAddress);
    }
}
