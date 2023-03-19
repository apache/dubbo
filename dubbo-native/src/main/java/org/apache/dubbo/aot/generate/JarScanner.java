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

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A scanner that scan the dependent jar packages
 * to obtain the classes source and resources in them.
 */
public class JarScanner {

    private static final String PACKAGE_NAME_PREFIX = "org/apache/dubbo";

    private final Map<String, String> classNameCache;

    private Map<String, Class<?>> classesCache;

    private final List<String> resourcePathCache;


    protected Map<String, Class<?>> getClasses() {
        if (classesCache == null || classesCache.size() == 0) {
            this.classesCache = forNames(classNameCache.values());
        }
        return classesCache;
    }

    public JarScanner() {
        classNameCache = new HashMap<>();
        resourcePathCache = new ArrayList<>();
        scanURL(PACKAGE_NAME_PREFIX);
    }

    protected Map<String, Class<?>> forNames(Collection<String> classNames) {
        Map<String, Class<?>> classes = new HashMap<>();
        classNames.forEach((it) -> {
            try {
                Class<?> c = Class.forName(it);
                classes.put(it, c);
            } catch (Throwable ignored) {
            }
        });
        return classes;
    }


    private void scanURL(String prefixName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(prefixName);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();
                if ("file".equals(protocol)) {
                    scanFile(resource.getPath());
                } else if ("jar".equals(protocol)) {
                    JarFile jar = ((JarURLConnection) resource.openConnection()).getJarFile();
                    scanJar(jar);
                }
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private void scanFile(String resource) {
        File directory = new File(resource);
        File[] listFiles = directory.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                System.out.println("scanFile: " + file.getPath());
                if (file.isDirectory()) {
                    scanFile(file.getPath());
                } else {
                    String path = file.getPath();
                    if (matchedDubboClasses(path)) {
                        classNameCache.put(path, toClassName(path));
                    }
                }
            }
        }
    }

    private void scanJar(JarFile jar) {
        Enumeration<JarEntry> entry = jar.entries();
        JarEntry jarEntry;
        String name;
        while (entry.hasMoreElements()) {
            jarEntry = entry.nextElement();
            name = jarEntry.getName();

            if (name.charAt(0) == '/') {
                name = name.substring(1);
            }

            if (jarEntry.isDirectory()) {
                continue;
            }

            if (matchedDubboClasses(name)) {
                classNameCache.put(name, toClassName(name));
            } else {
                resourcePathCache.add(name);
            }

        }
    }

    protected List<String> getResourcePath() {
        return resourcePathCache;
    }

    private boolean matchedDubboClasses(String path) {
        return path.startsWith(PACKAGE_NAME_PREFIX) && path.endsWith(".class");
    }

    private String toClassName(String path) {
        return path.substring(0, path.length() - 6).replace(File.separator, ".");
    }

}
