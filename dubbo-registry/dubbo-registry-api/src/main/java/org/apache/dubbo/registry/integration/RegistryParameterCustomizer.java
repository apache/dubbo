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

import org.apache.dubbo.common.extension.SPI;

import java.util.Map;

import static org.apache.dubbo.common.extension.ExtensionScope.APPLICATION;

/**
 * Customize parameters for interface-level registration
 */
@SPI(scope = APPLICATION)
public interface RegistryParameterCustomizer {

    /**
     * Customize register extra metadata.
     * The key needs to be excluded from the parametersExcluded list or prefixesExcluded list.
     * Check the parametersExcluded list and prefixesExcluded list of other {@link RegistryParameterCustomizer} implementations。
     *
     * @return map of extra parameter
     */
    Map<String, String> getExtraParameter();

    /**
     * params that need to be sent to registry center.
     *
     * @return arrays of keys
     */
    String[] parametersIncluded();

    /**
     * params that need to be excluded before sending to registry center.
     *
     * @return arrays of keys
     */
    String[] parametersExcluded();

    /**
     * params start with include prefix that need to be sent to registry center.
     *
     * @return arrays of prefixes
     */
    String[] prefixesIncluded();

    /**
     * params start with exclude prefix that need to be excluded before sending to registry center.
     *
     * @return arrays of prefixes
     */
    String[] prefixesExcluded();

}
