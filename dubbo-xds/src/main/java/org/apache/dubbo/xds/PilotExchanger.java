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
package org.apache.dubbo.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.directory.XdsDirectory;
import org.apache.dubbo.xds.resource.XdsResourceType;
import org.apache.dubbo.xds.resource.update.ResourceUpdate;

import java.util.Set;

public class PilotExchanger {

    private int pollingTimeout;
    private ApplicationModel applicationModel;
    protected final AdsObserver adsObserver;

    private final Set<String> domainObserveRequest = new ConcurrentHashSet<String>();

    private static PilotExchanger GLOBAL_PILOT_EXCHANGER = null;

    protected PilotExchanger(URL url) {
        this.pollingTimeout = url.getParameter("pollingTimeout", 10);
        adsObserver = new AdsObserver(url, NodeBuilder.build());
        this.applicationModel = url.getOrDefaultApplicationModel();
    }

    public <T extends ResourceUpdate> void subscribeXdsResource(
            String resourceName, XdsResourceType<T> resourceType, XdsResourceListener<T> resourceListener) {
        if (!adsObserver.hasSubscribed(resourceType)) {
            adsObserver.saveSubscribedType(resourceType);
        }

        XdsRawResourceProtocol<T> xdsProtocol = adsObserver.addListener(resourceName, resourceType);
        if (xdsProtocol != null) {
            xdsProtocol.subscribeResource(resourceName, resourceType, resourceListener);
        }
    }

    public void unSubscribeXdsResource(String clusterName, XdsDirectory listener) {}

    public static PilotExchanger initialize(URL url) {
        synchronized (PilotExchanger.class) {
            if (GLOBAL_PILOT_EXCHANGER != null) {
                return GLOBAL_PILOT_EXCHANGER;
            }
            return (GLOBAL_PILOT_EXCHANGER = new PilotExchanger(url));
        }
    }

    public static PilotExchanger getInstance() {
        synchronized (PilotExchanger.class) {
            return GLOBAL_PILOT_EXCHANGER;
        }
    }

    public static PilotExchanger createInstance(URL url) {
        return new PilotExchanger(url);
    }

    public static boolean isEnabled() {
        return GLOBAL_PILOT_EXCHANGER != null;
    }

    public void destroy() {
        this.adsObserver.destroy();
    }
}
