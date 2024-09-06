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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition;

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.protocol.tri.TripleConstants;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

import java.util.Objects;

public final class ServiceGroupVersionCondition implements Condition<ServiceGroupVersionCondition, HttpRequest> {

    private final String group;
    private final String version;

    public ServiceGroupVersionCondition(String group, String version) {
        this.group = group;
        this.version = version;
    }

    @Override
    public ServiceGroupVersionCondition combine(ServiceGroupVersionCondition other) {
        return this;
    }

    @Override
    public ServiceGroupVersionCondition match(HttpRequest request) {
        if (TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT) {
            return this;
        }

        String group = getHeader(request, TripleHeaderEnum.SERVICE_GROUP, RestConstants.HEADER_SERVICE_GROUP);
        if (!Objects.equals(group, this.group)) {
            return null;
        }

        String version = getHeader(request, TripleHeaderEnum.SERVICE_VERSION, RestConstants.HEADER_SERVICE_VERSION);
        if (!Objects.equals(version, this.version)) {
            return null;
        }

        return this;
    }

    @Override
    public int compareTo(ServiceGroupVersionCondition other, HttpRequest request) {
        if (!TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT) {
            return 0;
        }

        String group = getHeader(request, TripleHeaderEnum.SERVICE_GROUP, RestConstants.HEADER_SERVICE_GROUP);
        String version = getHeader(request, TripleHeaderEnum.SERVICE_VERSION, RestConstants.HEADER_SERVICE_VERSION);
        return getMatchLevel(other, group, version) - getMatchLevel(this, group, version);
    }

    private static String getHeader(HttpRequest request, TripleHeaderEnum en, String key) {
        String value = request.header(en.getKey());
        if (value == null) {
            value = request.header(key);
        }
        return value;
    }

    private static int getMatchLevel(ServiceGroupVersionCondition condition, String group, String version) {
        if (Objects.equals(group, condition.group)) {
            if (Objects.equals(version, condition.version)) {
                return 9;
            }
            if (Objects.equals(TripleConstants.DEFAULT_VERSION, condition.version)) {
                return 8;
            }
            return group == null ? 5 : 7;
        } else {
            if (Objects.equals(version, condition.version)) {
                return 6;
            }
            if (Objects.equals(TripleConstants.DEFAULT_VERSION, condition.version)) {
                return 4;
            }
            return 3;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != ServiceGroupVersionCondition.class) {
            return false;
        }
        ServiceGroupVersionCondition other = (ServiceGroupVersionCondition) obj;
        return Objects.equals(group, other.group) && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        return "ServiceVersionCondition{group='" + group + "', version='" + version + "'}";
    }
}
