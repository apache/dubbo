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

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ScopeModel;

/**
 * This class controls how Dubbo determines the logical service name
 * which is used for service discovery.
 * This is introduced to support Dubbo IDL services.
 * A legacy configuration key is also supported to ease migration from
 * earlier versions.
 */
public class ServiceNameConfig {

    // It defines how the service name is determined.
    public enum Mode {
        JAVA,
        IDL,
        BOTH
    }

    private static final String CONFIG_KEY = "dubbo.service.name.mode";
    private static final String LEGACY_KEY = "dubbo.application.use-idl-package-as-service-name";

    // Resolves the configured Mode from the environment.
    public static Mode getMode(ScopeModel scopeModel) {
        String value = ConfigurationUtils.getProperty(
                scopeModel, CONFIG_KEY, ConfigurationUtils.getProperty(scopeModel, LEGACY_KEY, "JAVA"));

        if (StringUtils.isEmpty(value)) {
            return Mode.JAVA;
        }

        try {
            return Mode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mode.JAVA;
        }
    }

    // Check if IDL service naming should be used.
    public static boolean useIdl(ScopeModel scopeModel) {
        Mode mode = getMode(scopeModel);
        return mode == Mode.IDL || mode == Mode.BOTH;
    }
}
