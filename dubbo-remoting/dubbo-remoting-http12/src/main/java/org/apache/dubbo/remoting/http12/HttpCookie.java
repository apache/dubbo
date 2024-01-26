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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.StringUtils;

public final class HttpCookie {

    private final String name;
    private String value;
    private String domain;
    private String path;
    private long maxAge = Long.MIN_VALUE;
    private boolean secure;
    private boolean httpOnly;
    private String sameSite;

    public HttpCookie(String name, String value) {
        name = StringUtils.trim(name);
        Assert.notEmptyString(name, "name is required");
        this.name = name;
        setValue(value);
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    public void setValue(String value) {
        Assert.notNull(name, "value can not be null");
        this.value = value;
    }

    public String domain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String path() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long maxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public boolean secure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean httpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public String sameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(name).append('=').append(value);
        if (domain != null) {
            buf.append(", domain=").append(domain);
        }
        if (path != null) {
            buf.append(", path=").append(path);
        }
        if (maxAge >= 0) {
            buf.append(", maxAge=").append(maxAge).append('s');
        }
        if (secure) {
            buf.append(", secure");
        }
        if (httpOnly) {
            buf.append(", HTTPOnly");
        }
        if (sameSite != null) {
            buf.append(", SameSite=").append(sameSite);
        }
        return buf.toString();
    }
}
