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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.kubernetes.KubeEnv;
import org.apache.dubbo.xds.security.api.ServiceAccountSource;

import java.nio.charset.StandardCharsets;

public class LocalSource implements ServiceAccountSource {

    private final KubeEnv kubeEnv;

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(LocalSource.class);

    public LocalSource(ApplicationModel applicationModel) {
        this.kubeEnv = applicationModel.getBeanFactory().getBean(KubeEnv.class);
    }

    @Override
    public String getJwt(URL url) {
        try {
            return new String(kubeEnv.getServiceAccountToken(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("", "", "Failed to read ServiceAccount from KubeEnv.", "", e);
            return "";
        }
    }
}
