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
package org.apache.dubbo.aot.generate;


import java.util.Set;
import java.util.stream.Collectors;

/**
 * A scanner for processing and filtering specific resource.
 */
public class ResourceScanner extends JarScanner {

    private static final String DUBBO_INTERNAL_RESOURCE_DIRECTORY = "META-INF/dubbo/internal/";

    private static final String DUBBO_RESOURCE_DIRECTORY = "META-INF/dubbo/";

    private static final String SERVICES_RESOURCE_DIRECTORY = "META-INF/services/";

    private static final String SECURITY_RESOURCE_DIRECTORY = "security/";

    public static final ResourceScanner INSTANCE = new ResourceScanner();

    public Set<String> distinctSpiResource() {
        return getResourcePath().stream().distinct().filter(this::matchedSpiResource).collect(Collectors.toSet());
    }

    public Set<String> distinctSecurityResource() {
        return getResourcePath().stream().distinct().filter(this::matchedSecurityResource).collect(Collectors.toSet());
    }

    private boolean matchedSecurityResource(String path) {
        return path.startsWith(SECURITY_RESOURCE_DIRECTORY);
    }

    private boolean matchedSpiResource(String path) {
        return path.startsWith(DUBBO_INTERNAL_RESOURCE_DIRECTORY)
            || path.startsWith(DUBBO_RESOURCE_DIRECTORY)
            || path.startsWith(SERVICES_RESOURCE_DIRECTORY);
    }


}
