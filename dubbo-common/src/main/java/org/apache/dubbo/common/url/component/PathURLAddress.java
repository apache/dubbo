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

import java.util.Objects;

public class PathURLAddress extends URLAddress {
    private String protocol;
    private String username;
    private String password;
    private String path;

    private transient String address;
    private transient String ip;

    public PathURLAddress(String protocol, String username, String password, String path, String host, int port) {
        this(protocol, username, password, path, host, port, null);
    }

    public PathURLAddress(String protocol, String username, String password, String path, String host, int port, String rawAddress) {
        super(host, port, rawAddress);

        this.protocol = protocol;
        this.username = username;
        this.password = password;

        // trim the beginning "/"
        while (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path != null) {
            path = path.intern();
        }
        this.path = path;
    }

    public String getProtocol() {
        return protocol;
    }

    public URLAddress setProtocol(String protocol) {
        return new PathURLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    public String getUsername() {
        return username;
    }

    public URLAddress setUsername(String username) {
        return new PathURLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    public String getPassword() {
        return password;
    }

    public PathURLAddress setPassword(String password) {
        return new PathURLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    public String getPath() {
        return path;
    }

    public PathURLAddress setPath(String path) {
        return new PathURLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    @Override
    public URLAddress setHost(String host) {
        return new PathURLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    @Override
    public URLAddress setPort(int port) {
        return new PathURLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    @Override
    public URLAddress setAddress(String host, int port) {
        return new PathURLAddress(protocol, username, password, path, host, port, rawAddress);
    }

    public String getAddress() {
        if (address == null) {
            address = getAddress(getHost(), getPort());
        }
        return address;
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
            ip = NetUtils.getIpByHost(getHost());
        }
        return ip;
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
//
//        if (StringUtils.isNotEmpty(username)) {
//            buf.append(username);
//            if (StringUtils.isNotEmpty(password)) {
//                buf.append(":");
//                buf.append(password);
//            }
//            buf.append("@");
//        }

        if (StringUtils.isNotEmpty(host)) {
            buf.append(host);
            if (port > 0) {
                buf.append(':');
                buf.append(port);
            }
        }

        if (StringUtils.isNotEmpty(path)) {
            buf.append('/');
            buf.append(path);
        }

        return buf.toString();
    }

}
