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
package org.apache.dubbo.xds.registry;

import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.InstanceAddressURL;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Objects;

public final class XdsServiceInstance extends DefaultServiceInstance {
    private final XdsEndpoint endpoint;

    public XdsServiceInstance(XdsEndpoint endpoint, MetadataInfo metadata, ApplicationModel applicationModel) {
        super(metadata.getApp(), endpoint.getHost(), endpoint.getPort(), applicationModel);
        this.endpoint = endpoint;
        setServiceMetadata(metadata);
        getMetadata().put("xds.target", endpoint.getTarget().toString());
        getMetadata().put("xds.cluster", endpoint.getCluster().getName());
    }

    public XdsEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public String getAddressIdentity() {
        return endpoint.identity();
    }

    @Override
    public InstanceAddressURL toURL(String protocol) {
        InstanceAddressURL url = super.toURL(protocol);
        url.putAttribute(XdsEndpoint.ATTRIBUTE, endpoint);
        return url;
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof XdsServiceInstance
                && endpoint.equals(((XdsServiceInstance) value).endpoint)
                && getServiceMetadata().equals(((XdsServiceInstance) value).getServiceMetadata());
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, getServiceMetadata());
    }
}
