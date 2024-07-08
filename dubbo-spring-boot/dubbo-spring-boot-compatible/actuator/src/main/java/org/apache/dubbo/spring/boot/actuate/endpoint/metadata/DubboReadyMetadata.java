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
package org.apache.dubbo.spring.boot.actuate.endpoint.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.qos.probe.ReadinessProbe;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Dubbo ready
 *
 * @since 3.3.0
 */
@Component
public class DubboReadyMetadata extends AbstractDubboMetadata {

    @Autowired
    public ApplicationModel applicationModel;

    public Map<String, Object> ready() {
        Map<String, Object> readyInfo = new LinkedHashMap<>();

        String config = applicationModel.getFrameworkModel()
                .getApplicationModels()
                .stream()
                .map(applicationModel -> applicationModel.getApplicationConfigManager()
                        .getApplication())
                .map(o -> o.orElse(null))
                .filter(Objects::nonNull)
                .map(ApplicationConfig::getReadinessProbe)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
        URL url = URL.valueOf("application://")
                .addParameter(CommonConstants.QOS_READY_PROBE_EXTENSION, config);
        List<ReadinessProbe> readinessProbes = applicationModel.getFrameworkModel()
                .getExtensionLoader(ReadinessProbe.class)
                .getActivateExtension(url, CommonConstants.QOS_READY_PROBE_EXTENSION);
        if (!readinessProbes.isEmpty()) {
            for (ReadinessProbe readinessProbe : readinessProbes) {
                if (!readinessProbe.check()) {
                    readyInfo.put(applicationModel.getApplicationName(), "false");
                    return readyInfo;
                }
            }
        }
        readyInfo.put(applicationModel.getApplicationName(), "true");
        return readyInfo;
    }
}
