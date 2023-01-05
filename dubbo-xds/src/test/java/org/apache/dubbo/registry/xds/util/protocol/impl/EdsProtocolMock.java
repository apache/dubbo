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

package org.apache.dubbo.registry.xds.util.protocol.impl;

import io.envoyproxy.envoy.config.core.v3.Node;
import org.apache.dubbo.registry.xds.util.XdsChannel;
import org.apache.dubbo.registry.xds.util.protocol.message.EndpointResult;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class EdsProtocolMock extends EdsProtocol{

    public EdsProtocolMock(XdsChannel xdsChannel, Node node, int pollingTimeout, ApplicationModel applicationModel) {
        super(xdsChannel, node, pollingTimeout, applicationModel);
    }

    public Map<String, EndpointResult> getResourcesMap() {
        return resourcesMap;
    }

    public void setResourcesMap(Map<String, EndpointResult> resourcesMap) {
        this.resourcesMap = resourcesMap;
    }

    public void setConsumerObserveMap(Map<Set<String>, List<Consumer<Map<String, EndpointResult>>>> consumerObserveMap) {
        this.consumerObserveMap = consumerObserveMap;
    }

    public ResponseObserverMock getResponseObserve() {
        return new ResponseObserverMock();
    }

    public void setObserveResourcesName(Set<String> observeResourcesName) {
        this.observeResourcesName = observeResourcesName;
    }
    class ResponseObserverMock extends ResponseObserver {

    }
}
