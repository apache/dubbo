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
import org.apache.dubbo.metrics.registry.event.type.ApplicationType;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.metrics.registry.RegistryConstants.ATTACHMENT_KEY_DIR_NUM;

public class DirectorSupport {

    public static RegistryEvent disable(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.D_DISABLE, null, null));
    }

    public static RegistryEvent valid(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.D_VALID, null, null));
    }

    public static RegistryEvent unValid(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.D_UN_VALID, null, null));
    }

    public static RegistryEvent current(ApplicationModel applicationModel, int num) {
        RegistryEvent ddEvent = new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.D_CURRENT, null, null));
        ddEvent.putAttachment(ATTACHMENT_KEY_DIR_NUM, num);
        return ddEvent;
    }

    public static RegistryEvent recover(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.D_RECOVER_DISABLE, null, null));
    }
}
