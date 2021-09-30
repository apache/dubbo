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
package org.apache.dubbo.config.context;

/**
 * Config processing mode for unique config type, e.g. ApplicationConfig, ModuleConfig, MonitorConfig, SslConfig, MetricsConfig
 * @see ConfigManager#uniqueConfigTypes
 */
public enum ConfigMode {
    /**
     * Strict mode: accept only one config for unique config type, throw exceptions if found more than one configs for an unique config type.
     */
    STRICT,

    /**
     * Override mode: accept last config, override previous config
     */
    OVERRIDE,

    /**
     * Ignore mode: accept first config, ignore later configs
     */
    IGNORE
}
