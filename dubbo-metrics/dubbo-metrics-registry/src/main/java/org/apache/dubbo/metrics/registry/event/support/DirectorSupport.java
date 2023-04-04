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

package org.apache.dubbo.metrics.registry.event.support;

import org.apache.dubbo.metrics.model.MetricsLevel;
import org.apache.dubbo.metrics.model.TypeWrapper;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.metrics.registry.event.type.ServiceType;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

import static org.apache.dubbo.metrics.registry.RegistryConstants.ATTACHMENT_KEY_LAST_NUM_MAP;
import static org.apache.dubbo.metrics.registry.RegistryConstants.ATTACHMENT_KEY_SERVICE;

public class DirectorSupport {

    public static RegistryEvent disable(ApplicationModel applicationModel, String serviceKey) {
        return addServiceKey(new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.SERVICE, ServiceType.D_DISABLE, null, null)), serviceKey);
    }

    public static RegistryEvent valid(ApplicationModel applicationModel, String serviceKey) {
        return addServiceKey(new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.SERVICE, ServiceType.D_VALID, null, null)), serviceKey);
    }

    public static RegistryEvent unValid(ApplicationModel applicationModel, String serviceKey) {
        return addServiceKey(new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.SERVICE, ServiceType.D_UN_VALID, null, null)), serviceKey);
    }

    public static RegistryEvent current(ApplicationModel applicationModel, String serviceKey, Map<String, Integer> serviceNumMap) {
        RegistryEvent ddEvent = new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.SERVICE, ServiceType.D_CURRENT, null, null));
        ddEvent.putAttachment(ATTACHMENT_KEY_LAST_NUM_MAP, serviceNumMap);
        ddEvent.putAttachment(ATTACHMENT_KEY_SERVICE, serviceKey);
        return ddEvent;
    }

    public static RegistryEvent recover(ApplicationModel applicationModel, String serviceKey) {
        return addServiceKey(new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.SERVICE, ServiceType.D_RECOVER_DISABLE, null, null)), serviceKey);
    }

    private static RegistryEvent addServiceKey(RegistryEvent event, String serviceKey) {
        event.putAttachment(ATTACHMENT_KEY_SERVICE, serviceKey);
        return event;
    }
}
