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
import org.apache.dubbo.xds.resource.XdsResourceType;
import org.apache.dubbo.xds.resource.update.ResourceUpdate;

public class PilotExchanger {

    protected final AdsObserver adsObserver;

    private static PilotExchanger GLOBAL_PILOT_EXCHANGER = null;

    protected PilotExchanger() {
        adsObserver = new AdsObserver();
    }

    public <T extends ResourceUpdate> void subscribeXdsResource(
            String resourceName, XdsResourceType<T> resourceType, XdsResourceListener<T> resourceListener) {
        if (!adsObserver.hasSubscribed(resourceType)) {
            adsObserver.saveSubscribedType(resourceType);
        }

        adsObserver.addListener(resourceName, resourceType, resourceListener);
    }

    public static PilotExchanger getInstance() {
        synchronized (PilotExchanger.class) {
            if (GLOBAL_PILOT_EXCHANGER != null) {
                return GLOBAL_PILOT_EXCHANGER;
            }
            return (GLOBAL_PILOT_EXCHANGER = new PilotExchanger());
        }
    }

    public void destroy() {
        this.adsObserver.destroy();
    }
}
