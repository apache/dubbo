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
package org.apache.dubbo.config;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.HashMap;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_ROUTER_ENV;
import static org.apache.dubbo.common.constants.CommonConstants.ROUTER_ENV_KEY;


@Activate
public class EnvRouterAppendParameter implements AppendParametersComponent {
    @Override
    public void appendReferParameters(ReferenceConfig referenceConfig) {
        String routerKey = referenceConfig.getEnv();
        if (StringUtils.isEmpty(routerKey)) {
            routerKey = referenceConfig.getConsumer().getEnv();
        }
        if (StringUtils.isEmpty(routerKey)) {
            routerKey = DEFAULT_ROUTER_ENV;
        }
        String routerValue = System.getenv(routerKey);
        if (StringUtils.isNotEmpty(routerValue)) {
            if (referenceConfig.getParameters() == null) {
                referenceConfig.setParameters( new HashMap<>());
            }
            referenceConfig.getParameters().put(ROUTER_ENV_KEY,routerValue);
        }
    }
    @Override
    public void appendExportParameters(ServiceConfig serviceConfig) {
        String routerKey = serviceConfig.getEnv();
        if (StringUtils.isEmpty(routerKey)){
            routerKey = serviceConfig.getProvider().getEnv();
        }
        if (StringUtils.isEmpty(routerKey)){
            routerKey = DEFAULT_ROUTER_ENV;
        }
        String routerValue = System.getenv(routerKey);
        if (StringUtils.isNotEmpty(routerValue)) {
            if (serviceConfig.getParameters() == null) {
                serviceConfig.setParameters( new HashMap<>());
            }
            serviceConfig.getParameters().put(ROUTER_ENV_KEY,routerValue);
        }
    }

}
