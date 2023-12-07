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
package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.URL;

import java.util.Collections;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.ALIVE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.BACKGROUND_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CORE_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE;
import static org.apache.dubbo.common.constants.CommonConstants.HIDE_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.QUEUES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_LOCAL_FILE_CACHE_ENABLED;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.apache.dubbo.common.constants.FilterConstants.VALIDATION_KEY;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTER_MODE_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.rpc.Constants.INTERFACES;

public class DefaultRegistryParameterCustomizer implements RegistryParameterCustomizer {

    @Override
    public Map<String, String> getExtraParameter(URL providerUrl, URL registryUrl) {
        return Collections.emptyMap();
    }

    @Override
    public String[] parametersExcluded(URL providerUrl, URL registryUrl) {
        return new String[] {
            MONITOR_KEY,
            BIND_IP_KEY,
            BIND_PORT_KEY,
            QOS_ENABLE,
            QOS_HOST,
            QOS_PORT,
            ACCEPT_FOREIGN_IP,
            VALIDATION_KEY,
            INTERFACES,
            REGISTER_MODE_KEY,
            PID_KEY,
            REGISTRY_LOCAL_FILE_CACHE_ENABLED,
            EXECUTOR_MANAGEMENT_MODE,
            BACKGROUND_KEY,
            ANYHOST_KEY,
            THREAD_NAME_KEY,
            THREADPOOL_KEY,
            ALIVE_KEY,
            QUEUES_KEY,
            CORE_THREADS_KEY,
            THREADS_KEY
        };
    }

    @Override
    public String[] prefixesExcluded(URL providerUrl, URL registryUrl) {
        return new String[] {HIDE_KEY_PREFIX};
    }
}
