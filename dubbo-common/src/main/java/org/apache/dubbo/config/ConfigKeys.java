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

import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.context.ConfigMode;

/**
 * External config keys list
 * @see org.apache.dubbo.spring.boot.autoconfigure.DubboConfigurationProperties
 */
public interface ConfigKeys {

    /**
     * The basePackages to scan , the multiple-value is delimited by comma
     * @see org.apache.dubbo.config.spring.context.annotation.EnableDubbo#scanBasePackages()
     */
    String DUBBO_SCAN_BASE_PACKAGES = "dubbo.scan.base-packages";

    /**
     * Change dubbo config mode, available values from {@link ConfigMode}. Default value is {@link ConfigMode#STRICT}.
     * @see ConfigMode
     * @see ConfigManager#configMode
     */
    String DUBBO_CONFIG_MODE = "dubbo.config.mode";

    /**
     * Ignore invalid method config. Default value is false.
     */
    String DUBBO_CONFIG_IGNORE_INVALID_METHOD_CONFIG = "dubbo.config.ignore-invalid-method-config";

    /**
     * Ignore duplicated interface (service/reference) config. Default value is false.
     */
    String DUBBO_CONFIG_IGNORE_DUPLICATED_INTERFACE = "dubbo.config.ignore-duplicated-interface";

}
