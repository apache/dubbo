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

import org.apache.dubbo.common.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;

public class URLAddress {
    private final String rawAddress;

    private final String protocol;
    private final String username;
    private final String password;
    private final String path;
    private final String host;
    private final int port;

    public URLAddress(String protocol, String username, String password, String path, String host, int port, String rawAddress) {
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.path = path;
        this.host = host;
        this.port = port;

        this.rawAddress = rawAddress;
    }

    public String getProtocol() {
        return protocol;
    }

    public URLAddress setProtocol(String protocol) {
        return new URLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    public String getUsername() {
        return username;
    }

    public URLAddress setUsername(String username) {
        return new URLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    public String getPassword() {
        return password;
    }

    public URLAddress setPassword(String password) {
        return new URLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    public String getPath() {
        return path;
    }

    public URLAddress setPath(String path) {
        return new URLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    public String getHost() {
        return host;
    }

    public URLAddress setHost(String host) {
        return new URLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    public int getPort() {
        return port;
    }

    public URLAddress setPort(int port) {
        return new URLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    public URLAddress setAddress(String host, int port) {
        return new URLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    public String getRawAddress() {
        return rawAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, username, password, path, host, port);
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
        if (StringUtils.isNotEmpty(protocol)) {
            buf.append(protocol);
            buf.append("://");
        }

        if (StringUtils.isNotEmpty(host)) {
            buf.append(host);
            if (port > 0) {
                buf.append(":");
                buf.append(port);
            }
        }

        if (StringUtils.isNotEmpty(path)) {
            buf.append("/");
            buf.append(path);
        }

        return buf.toString();
    }

    public static URLAddress parse(String rawAddress, String defaultProtocol, boolean encoded) {
        try {
            if (encoded) {
                rawAddress = URLDecoder.decode(rawAddress, "UTF-8");
            }
            return parse(rawAddress, defaultProtocol);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static URLAddress parse(String rawAddress, String defaultProtocol) {
        String protocol = defaultProtocol;
        String copyOfRawAddress = rawAddress;
        String path = null, username = null, password = null, host = null;
        int port = 0;
        int i = rawAddress.indexOf("://");
        if (i >= 0) {
            if (i == 0) {
                throw new IllegalStateException("url missing protocol: \"" + rawAddress + "\"");
            }
            protocol = rawAddress.substring(0, i);
            rawAddress = rawAddress.substring(i + 3);
        } else {
            // case: file:/path/to/file.txt
            i = rawAddress.indexOf(":/");
            if (i >= 0) {
                if (i == 0) {
                    throw new IllegalStateException("url missing protocol: \"" + rawAddress + "\"");
                }
                protocol = rawAddress.substring(0, i);
                rawAddress = rawAddress.substring(i + 1);
            }
        }

        i = rawAddress.indexOf('/');
        if (i >= 0) {
            path = rawAddress.substring(i + 1);
            rawAddress = rawAddress.substring(0, i);
        }
        i = rawAddress.lastIndexOf('@');
        if (i >= 0) {
            username = rawAddress.substring(0, i);
            int j = username.indexOf(':');
            if (j >= 0) {
                password = username.substring(j + 1);
                username = username.substring(0, j);
            }
            rawAddress = rawAddress.substring(i + 1);
        }
        i = rawAddress.lastIndexOf(':');
        if (i >= 0 && i < rawAddress.length() - 1) {
            if (rawAddress.lastIndexOf('%') > i) {
                // ipv6 address with scope id
                // e.g. fe80:0:0:0:894:aeec:f37d:23e1%en0
                // see https://howdoesinternetwork.com/2013/ipv6-zone-id
                // ignore
            } else {
                port = Integer.parseInt(rawAddress.substring(i + 1));
                host = rawAddress.substring(0, i);
            }
        }
        return new URLAddress(protocol, username, password, path, host, port, copyOfRawAddress);
    }

    public static URLAddress valueOf(
            String protocol,
            String username,
            String password,
            String host,
            int port,
            String path
    ) {
        port = Math.max(port, 0);
        // trim the beginning "/"
        while (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        return new URLAddress(protocol, username, password, path, host, port, null);
    }
}
