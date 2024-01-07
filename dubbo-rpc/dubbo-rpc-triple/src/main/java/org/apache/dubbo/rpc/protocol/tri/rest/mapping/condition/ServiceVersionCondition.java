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
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

public final class ServiceVersionCondition implements Condition<ServiceVersionCondition, HttpRequest> {

    private final String group;
    private final String version;

    public ServiceVersionCondition(String group, String version) {
        this.group = group;
        this.version = version;
    }

    @Override
    public ServiceVersionCondition combine(ServiceVersionCondition other) {
        return this;
    }

    @Override
    public ServiceVersionCondition match(HttpRequest request) {
        String group = request.header(TripleHeaderEnum.SERVICE_GROUP.name());
        if (group == null) {
            group = request.header(RestConstants.HEADER_SERVICE_GROUP);
        }
        if (group != null && !group.equals(this.group)) {
            return null;
        }

        String version = request.header(TripleHeaderEnum.SERVICE_VERSION.name());
        if (version == null) {
            version = request.header(RestConstants.HEADER_SERVICE_VERSION);
        }

        if (version != null && !version.equals(this.version)) {
            return null;
        }
        return this;
    }

    @Override
    public int compareTo(ServiceVersionCondition other, HttpRequest request) {
        return 0;
    }

    @Override
    public String toString() {
        return "ServiceVersionCondition{" + "group='" + group + '\'' + ", version='" + version + '\'' + '}';
    }
}
