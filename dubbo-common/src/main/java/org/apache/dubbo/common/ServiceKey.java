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

import java.util.Objects;

public class ServiceKey {
    private final String interfaceName;
    private final String group;
    private final String version;

    public ServiceKey(String interfaceName, String version, String group) {
        this.interfaceName = interfaceName;
        this.group = group;
        this.version = version;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceKey that = (ServiceKey) o;
        return Objects.equals(interfaceName, that.interfaceName) && Objects.equals(group, that.group) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interfaceName, group, version);
    }

    @Override
    public String toString() {
        return BaseServiceMetadata.buildServiceKey(interfaceName, group, version);
    }
}
