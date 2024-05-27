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
package org.apache.dubbo.config.spring.aot;

import org.apache.dubbo.common.aot.NativeDetector;

import org.springframework.core.SpringProperties;

public abstract class AotWithSpringDetector {

    /**
     * System property that indicates the application should run with AOT
     * generated artifacts. If such optimizations are not available, it is
     * recommended to throw an exception rather than fall back to the regular
     * runtime behavior.
     */
    public static final String AOT_ENABLED = "spring.aot.enabled";

    private static final String AOT_PROCESSING = "spring.aot.processing";

    /**
     * Determine whether AOT optimizations must be considered at runtime. This
     * is mandatory in a native image but can be triggered on the JVM using
     * the {@value #AOT_ENABLED} Spring property.
     *
     * @return whether AOT optimizations must be considered
     */
    public static boolean useGeneratedArtifacts() {
        return (NativeDetector.inNativeImage() || SpringProperties.getFlag(AOT_ENABLED));
    }

    public static boolean isAotProcessing() {
        return (NativeDetector.inNativeImage() || SpringProperties.getFlag(AOT_PROCESSING));
    }
}
