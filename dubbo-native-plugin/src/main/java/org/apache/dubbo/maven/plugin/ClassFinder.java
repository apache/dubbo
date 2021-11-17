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
package org.apache.dubbo.maven.plugin;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassFinder {

    public Set<String> findClassSet(String packageName, Consumer<String> consumer) {
        packageName = packageName.replace(".", "/");
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration resources = classLoader.getResources(packageName);
            Set<String> result = new HashSet<>();
            while (resources.hasMoreElements()) {
                URL resource = (URL) resources.nextElement();
                if (resource != null) {
                    String protocol = resource.getProtocol();
                    if ("file".equals(protocol)) {
                        findClassesByFile(packageName, resource.getPath(), result);
                    } else if ("jar".equals(protocol)) {
                        JarFile jar = ((JarURLConnection) resource.openConnection()).getJarFile();
                        consumer.accept("findClassSet jar:" + jar.getName());
                        findClassesByJar(packageName, jar, result);
                    }
                }
            }
            return result;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private void findClassesByFile(String packageName, String resource, Set<String> result) {
        File directory = new File(resource);
        File[] listFiles = directory.listFiles();
        for (File file : listFiles) {
            if (file.isDirectory()) {
                findClassesByFile(packageName, file.getPath(), result);
            } else {
                String path = file.getPath();
                if (path.endsWith(".class")) {
                    int packageIndex = path.indexOf(packageName.replace("/", File.separator));
                    String classPath = path.substring(packageIndex, path.length() - 6);
                    result.add(classPath.replace(File.separator, "."));
                }
            }
        }
    }

    private static void findClassesByJar(String packageName, JarFile jar, Set<String> classes) {
        Enumeration<JarEntry> entry = jar.entries();
        JarEntry jarEntry;
        String name;
        while (entry.hasMoreElements()) {
            jarEntry = entry.nextElement();
            name = jarEntry.getName();
            if (name.charAt(0) == '/') {
                name = name.substring(1);
            }
            if (jarEntry.isDirectory() || !name.startsWith(packageName) || !name.endsWith(".class")) {
                continue;
            }
            String className = name.substring(0, name.length() - 6);
            classes.add(className.replace("/", "."));
        }
    }
}
