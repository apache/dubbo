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


import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A scanner for processing and filtering specific types of classes
 */
public class ClassSourceScanner extends JarScanner {

    public static final ClassSourceScanner INSTANCE = new ClassSourceScanner();

    /**
     *  Filter out the spi classes with adaptive annotations
     *  from all the class collections that can be loaded.
     * @return All spi classes with adaptive annotations
     */
    public List<Class<?>> spiClassesWithAdaptive() {
        Map<String, Class<?>> allClasses = getClasses();
        List<Class<?>> spiClasses = new ArrayList<>(allClasses.values()).stream().filter(it -> {
            if (null == it) {
                return false;
            }
            Annotation anno = it.getAnnotation(SPI.class);
            if (null == anno) {
                return false;
            }
            Optional<Method> optional = Arrays.stream(it.getMethods()).filter(it2 -> it2.getAnnotation(Adaptive.class) != null).findAny();
            return optional.isPresent();
        }).collect(Collectors.toList());

        return spiClasses;
    }

    /**
     * The required adaptive class.
     * For example: LoadBalance$Adaptive.class
     * @return adaptive class
     */
    public Map<String, Class<?>> adaptiveClasses() {
        List<String> res = spiClassesWithAdaptive().stream().map((c) -> c.getName() + "$Adaptive").collect(Collectors.toList());
        return forNames(res);
    }

    /**
     * The required configuration class, which is a subclass of AbstractConfig,
     * but which excludes abstract classes.
     * @return configuration class
     */
    public List<Class<?>> configClasses() {
        return getClasses().values().stream().filter(c -> AbstractConfig.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())).collect(Collectors.toList());
    }

    public Map<String, Class<?>> distinctSpiExtensionClasses(Set<String> spiResource) {

        Map<String, Class<?>> extensionClasses = new HashMap<>();
        spiResource.forEach((fileName) -> {
            Enumeration<URL> resources;
            try {
                resources = ClassLoader.getSystemResources(fileName);
                if (resources != null) {
                    while (resources.hasMoreElements()) {
                        extensionClasses.putAll(loadResource(resources.nextElement()));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return extensionClasses;
    }

    /**
     * Beans that need to be injected in advance in different ScopeModels.
     * For example, the RouterSnapshotSwitcher that needs to be injected when ClusterScopeModelInitializer executes initializeFrameworkModel
     * @return Beans that need to be injected in advance
     */
    public  List<Class<?>> scopeModelInitializer(){
        List<Class<?>> classes = new ArrayList<>();
        classes.addAll(FrameworkModel.defaultModel().getBeanFactory().getRegisteredClasses());
        classes.addAll(FrameworkModel.defaultModel().defaultApplication().getBeanFactory().getRegisteredClasses());
        classes.addAll(FrameworkModel.defaultModel().defaultApplication().getDefaultModule().getBeanFactory().getRegisteredClasses());
        return classes.stream().distinct().collect(Collectors.toList());
    }


    private Map<String, Class<?>> loadResource(URL resourceUrl) {
        Map<String, Class<?>> extensionClasses = new HashMap<>();
        try {
            List<String> newContentList = getResourceContent(resourceUrl);
            String clazz;
            for (String line : newContentList) {
                try {
                    int i = line.indexOf('=');
                    if (i > 0) {
                        clazz = line.substring(i + 1).trim();
                    } else {
                        clazz = line;
                    }
                    if (StringUtils.isNotEmpty(clazz)) {
                        extensionClasses.put(clazz, getClasses().get(clazz));
                    }
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
        }

        return extensionClasses;
    }

    private List<String> getResourceContent(URL resourceUrl) throws IOException {
        List<String> newContentList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() > 0) {
                    newContentList.add(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return newContentList;
    }


}
