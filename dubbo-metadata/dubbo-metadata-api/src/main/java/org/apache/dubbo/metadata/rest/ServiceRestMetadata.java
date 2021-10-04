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
package org.apache.dubbo.metadata.rest;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The metadata class for {@link RequestMetadata HTTP(REST) request} and
 * its binding Dubbo service metadata
 *
 * @since 2.7.6
 */
public class ServiceRestMetadata implements Serializable {

    private static final long serialVersionUID = -4549723140727443569L;

    private String serviceInterface;

    private String version;

    private String group;

    private Set<RestMethodMetadata> meta;

    public String getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(String serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Set<RestMethodMetadata> getMeta() {
        if (meta == null) {
            meta = new LinkedHashSet<>();
        }
        return meta;
    }

    public void setMeta(Set<RestMethodMetadata> meta) {
        this.meta = meta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceRestMetadata)) {
            return false;
        }
        ServiceRestMetadata that = (ServiceRestMetadata) o;
        return Objects.equals(getServiceInterface(), that.getServiceInterface()) &&
                Objects.equals(getVersion(), that.getVersion()) &&
                Objects.equals(getGroup(), that.getGroup()) &&
                Objects.equals(getMeta(), that.getMeta());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServiceInterface(), getVersion(), getGroup(), getMeta());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ServiceRestMetadata{");
        sb.append("serviceInterface='").append(serviceInterface).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", group='").append(group).append('\'');
        sb.append(", meta=").append(meta);
        sb.append('}');
        return sb.toString();
    }
}
