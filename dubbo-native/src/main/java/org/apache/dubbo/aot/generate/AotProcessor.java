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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * generate related self-adaptive code (native image does not support dynamic code generation. Therefore, code needs to be generated before compilation)
 */
public class AotProcessor {

    public static void main(String[] args) {
        String generatedSources = args[1];

        List<Class<?>> classes = ClassSourceScanner.INSTANCE.spiClassesWithAdaptive();
        NativeClassSourceWriter.INSTANCE.writeTo(classes, generatedSources);

        NativeConfigurationWriter writer = new NativeConfigurationWriter(Paths.get(args[2]), args[4], args[5]);

        ResourceConfigMetadataRepository resourceRepository = new ResourceConfigMetadataRepository();
        resourceRepository.registerIncludesPatterns(ResourceScanner.INSTANCE.distinctSpiResource().toArray(new String[]{}));
        resourceRepository.registerIncludesPatterns(ResourceScanner.INSTANCE.distinctSecurityResource().toArray(new String[]{}));
        writer.writeResourceConfig(resourceRepository);


        ReflectConfigMetadataRepository reflectRepository = new ReflectConfigMetadataRepository();
        reflectRepository
            .registerSpiExtensionType(new ArrayList<>(ClassSourceScanner.INSTANCE.distinctSpiExtensionClasses(ResourceScanner.INSTANCE.distinctSpiResource()).values()))
            .registerAdaptiveType(new ArrayList<>(ClassSourceScanner.INSTANCE.adaptiveClasses().values()))
            .registerBeanType(ClassSourceScanner.INSTANCE.scopeModelInitializer())
            .registerConfigType(ClassSourceScanner.INSTANCE.configClasses());
        writer.writeReflectionConfig(reflectRepository);


    }


}
