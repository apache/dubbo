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
package org.apache.dubbo.xds.resource_new.update;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.xds.resource_new.route.VirtualHost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RdsUpdate implements ResourceUpdate {
    // The list virtual hosts that make up the route table.
    final List<VirtualHost> virtualHosts;

    public RdsUpdate(List<VirtualHost> virtualHosts) {
        Assert.notNull(virtualHosts, "virtualHosts must not be null");
        this.virtualHosts = Collections.unmodifiableList(new ArrayList<>(virtualHosts));
    }

    @Override
    public String toString() {
        return "RdsUpdate{" + "virtualHosts=" + virtualHosts + '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(virtualHosts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RdsUpdate that = (RdsUpdate) o;
        return Objects.equals(virtualHosts, that.virtualHosts);
    }
}
