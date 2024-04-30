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
package org.apache.dubbo.xds.security;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;
import org.apache.dubbo.xds.kubernetes.KubeApiClient;
import org.apache.dubbo.xds.kubernetes.KubeEnv;
import org.apache.dubbo.xds.security.api.MeshCertProvider;
import org.apache.dubbo.xds.security.authz.rule.source.MapRuleFactory;

import java.io.IOException;

public class SecurityBeanConfig implements ScopeModelInitializer {

    private ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SecurityBeanConfig.class);

    @Override
    public void initializeFrameworkModel(FrameworkModel frameworkModel) {
        frameworkModel.getBeanFactory().getOrRegisterBean(MeshCertProvider.class);
    }

    @Override
    public void initializeApplicationModel(ApplicationModel applicationModel) {
        KubeEnv env = applicationModel.getBeanFactory().getOrRegisterBean(KubeEnv.class);
        try {
            if (env.getServiceAccountToken().length > 0) {
                applicationModel.getBeanFactory().getOrRegisterBean(KubeApiClient.class);
                applicationModel.getBeanFactory().getOrRegisterBean(MapRuleFactory.class);
            }
        } catch (IOException e) {
            logger.info("SecurityBeanConfig are not initialized because SA token not found.");
        }
    }

    @Override
    public void initializeModuleModel(ModuleModel moduleModel) {}
}
