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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.DubboServiceAddressURL;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.XdsResourceFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;

public class XdsRegistry extends FailbackRegistry {

    private XdsResourceFactory xdsResourceFactory = XdsResourceFactory.getInstance();
    private ApplicationModel applicationModel;

    public XdsRegistry(URL url) {
        super(url);
        this.applicationModel = url.getApplicationModel();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void doRegister(URL url) {}

    @Override
    public void doUnregister(URL url) {}

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        String providedBy = url.getParameter(PROVIDED_BY);
        xdsResourceFactory.subscribeApp(providedBy, (addresses -> {
            List<URL> instances = addresses.stream()
                    .map(address -> new DubboServiceAddressURL(address.getUrlAddress(), address.getUrlParam(), url, null))
                    .collect(Collectors.toList());
            listener.notify(instances);
        }));
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {}
}
