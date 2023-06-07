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

package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.config.ConfigPostProcessor;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.rpc.model.FrameworkModel;

import static org.apache.dubbo.common.constants.CommonConstants.SSL_ENABLED_KEY;

@Activate
public class SecurityConfigPostProcessor implements ConfigPostProcessor {
    private final FrameworkModel frameworkModel;

    public SecurityConfigPostProcessor(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public URL portProcessServiceConfig(ServiceConfig serviceConfig, URL url) {
        AuthenticationGovernor governor = frameworkModel.getBeanFactory().getBean(AuthenticationGovernor.class);
        if (governor == null) {
            return url;
        }

        if (url.getParameter(SSL_ENABLED_KEY, false)) {
            return url;
        }

        AuthPolicy authPolicy = governor.getPortPolicy(url.getPort());
        if (authPolicy == null || authPolicy == AuthPolicy.NONE) {
            return url;
        }

        return url.addParameter(SSL_ENABLED_KEY, true);
    }
}
