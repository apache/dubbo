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

import org.apache.dubbo.aot.api.JdkProxyDescriber;
import org.apache.dubbo.aot.api.ProxyDescriberRegistrar;
import org.apache.dubbo.aot.api.ReflectionTypeDescriberRegistrar;
import org.apache.dubbo.aot.api.ResourceBundleDescriber;
import org.apache.dubbo.aot.api.ResourceDescriberRegistrar;
import org.apache.dubbo.aot.api.ResourcePatternDescriber;
import org.apache.dubbo.aot.api.TypeDescriber;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
        resourceRepository.registerIncludesPatterns(
                ResourceScanner.INSTANCE.distinctSpiResource().toArray(new String[] {}));
        resourceRepository.registerIncludesPatterns(
                ResourceScanner.INSTANCE.distinctSecurityResource().toArray(new String[] {}));
        for (ResourcePatternDescriber resourcePatternDescriber : getResourcePatternDescribers()) {
            resourceRepository.registerIncludesPattern(resourcePatternDescriber);
        }
        for (ResourceBundleDescriber resourceBundleDescriber : getResourceBundleDescribers()) {
            resourceRepository.registerBundles(resourceBundleDescriber);
        }
        writer.writeResourceConfig(resourceRepository);

        ReflectConfigMetadataRepository reflectRepository = new ReflectConfigMetadataRepository();
        reflectRepository
                .registerSpiExtensionType(new ArrayList<>(ClassSourceScanner.INSTANCE
                        .distinctSpiExtensionClasses(ResourceScanner.INSTANCE.distinctSpiResource())
                        .values()))
                .registerAdaptiveType(new ArrayList<>(
                        ClassSourceScanner.INSTANCE.adaptiveClasses().values()))
                .registerBeanType(ClassSourceScanner.INSTANCE.scopeModelInitializer())
                .registerConfigType(ClassSourceScanner.INSTANCE.configClasses())
                .registerFieldType(getCustomClasses())
                .registerTypeDescriber(getTypes());
        writer.writeReflectionConfig(reflectRepository);

        ProxyConfigMetadataRepository proxyRepository = new ProxyConfigMetadataRepository();
        proxyRepository.registerProxyDescribers(getProxyDescribers());
        writer.writeProxyConfig(proxyRepository);
    }

    private static List<TypeDescriber> getTypes() {
        List<TypeDescriber> typeDescribers = new ArrayList<>();
        FrameworkModel.defaultModel()
                .defaultApplication()
                .getExtensionLoader(ReflectionTypeDescriberRegistrar.class)
                .getSupportedExtensionInstances()
                .forEach(reflectionTypeDescriberRegistrar -> {
                    List<TypeDescriber> describers = new ArrayList<>();
                    try {
                        describers = reflectionTypeDescriberRegistrar.getTypeDescribers();
                    } catch (Throwable e) {
                        // The ReflectionTypeDescriberRegistrar implementation classes are shaded, causing some unused
                        // classes to be loaded.
                        // When loading a dependent class may appear that cannot be found, it does not affect.
                        // ignore
                    }

                    typeDescribers.addAll(describers);
                });

        return typeDescribers;
    }

    private static List<ResourcePatternDescriber> getResourcePatternDescribers() {
        List<ResourcePatternDescriber> resourcePatternDescribers = new ArrayList<>();
        FrameworkModel.defaultModel()
                .defaultApplication()
                .getExtensionLoader(ResourceDescriberRegistrar.class)
                .getSupportedExtensionInstances()
                .forEach(reflectionTypeDescriberRegistrar -> {
                    List<ResourcePatternDescriber> describers = new ArrayList<>();
                    try {
                        describers = reflectionTypeDescriberRegistrar.getResourcePatternDescribers();
                    } catch (Throwable e) {
                        // The ResourceDescriberRegistrar implementation classes are shaded, causing some unused
                        // classes to be loaded.
                        // When loading a dependent class may appear that cannot be found, it does not affect.
                        // ignore
                    }

                    resourcePatternDescribers.addAll(describers);
                });

        return resourcePatternDescribers;
    }

    private static List<ResourceBundleDescriber> getResourceBundleDescribers() {
        List<ResourceBundleDescriber> resourceBundleDescribers = new ArrayList<>();
        FrameworkModel.defaultModel()
                .defaultApplication()
                .getExtensionLoader(ResourceDescriberRegistrar.class)
                .getSupportedExtensionInstances()
                .forEach(reflectionTypeDescriberRegistrar -> {
                    List<ResourceBundleDescriber> describers = new ArrayList<>();
                    try {
                        describers = reflectionTypeDescriberRegistrar.getResourceBundleDescribers();
                    } catch (Throwable e) {
                        // The ResourceDescriberRegistrar implementation classes are shaded, causing some unused
                        // classes to be loaded.
                        // When loading a dependent class may appear that cannot be found, it does not affect.
                        // ignore
                    }

                    resourceBundleDescribers.addAll(describers);
                });

        return resourceBundleDescribers;
    }

    private static List<JdkProxyDescriber> getProxyDescribers() {
        List<JdkProxyDescriber> jdkProxyDescribers = new ArrayList<>();
        FrameworkModel.defaultModel()
                .defaultApplication()
                .getExtensionLoader(ProxyDescriberRegistrar.class)
                .getSupportedExtensionInstances()
                .forEach(reflectionTypeDescriberRegistrar -> {
                    jdkProxyDescribers.addAll(reflectionTypeDescriberRegistrar.getJdkProxyDescribers());
                });

        return jdkProxyDescribers;
    }

    private static List<Class<?>> getCustomClasses() {
        Class<?>[] configClasses = new Class[] {
            CommonConstants.SystemProperty.class,
            CommonConstants.ThirdPartyProperty.class,
            CommonConstants.DubboProperty.class
        };
        return new ArrayList<>(Arrays.asList(configClasses));
    }
}
