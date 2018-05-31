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
package com.alibaba.dubbo.common;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ClassHelper;
import com.alibaba.dubbo.common.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Version
 */
public final class Version {

    private static final String DEFAULT_DUBBO_VERSION = "2.0.0";
    private static final Logger logger = LoggerFactory.getLogger(Version.class);
    private static final String VERSION = getVersion(Version.class, DEFAULT_DUBBO_VERSION);
    private static final String DUBBO_VERSION_PROPERTIES_PATH = "/dubboVersion.properties";
    private static final String DUBBO_VERSION_KEY = "dubbo.version";

    static {
        // check if there's duplicated jar
        Version.checkDuplicate(Version.class);
    }

    private Version() {
    }

    public static String getVersion() {
        return VERSION;
    }

    /**
     * get version from dubboVersion.properties filled by pom.xml
     *
     * @return
     */
    private static String getVersionFromConfigFile() {
        String version = null;
        try {
            InputStream inputStream = Version.class.getResourceAsStream(DUBBO_VERSION_PROPERTIES_PATH);
            Properties properties = new Properties();
            properties.load(inputStream);
            version = properties.getProperty(DUBBO_VERSION_KEY);
        } catch (IOException e) {
            logger.error("return version error " + e.getMessage(), e);
        }
        return version;
    }

    public static String getVersion(Class<?> cls, String defaultVersion) {
        try {
            // find version info from dubboVersion.properties
            String version = getVersionFromConfigFile();
            if (StringUtils.isNotEmpty(version)) {
                return version;
            }
            // find version info from MANIFEST.MF first
            version = cls.getPackage().getImplementationVersion();
            if (StringUtils.isNotEmpty(version)) {
                String specificationVersion = cls.getPackage().getSpecificationVersion();
                return StringUtils.isNotEmpty(specificationVersion) ? specificationVersion : defaultVersion;
            } else {
                // guess version from jar file name if nothing's found from from dubboVersion.properties and MANIFEST.MF
                CodeSource codeSource = cls.getProtectionDomain().getCodeSource();
                if (codeSource == null) {
                    logger.info("No codeSource for class " + cls.getName() + " when getVersion, use default version " + defaultVersion);
                } else {
                    String file = codeSource.getLocation().getFile();
                    if (file != null && file.length() > 0 && file.endsWith(".jar")) {
                        file = file.substring(0, file.length() - 4);
                        int i = file.lastIndexOf('/');
                        if (i >= 0) {
                            file = file.substring(i + 1);
                        }
                        i = file.indexOf("-");
                        if (i >= 0) {
                            file = file.substring(i + 1);
                        }
                        while (file.length() > 0 && !Character.isDigit(file.charAt(0))) {
                            i = file.indexOf("-");
                            if (i >= 0) {
                                file = file.substring(i + 1);
                            } else {
                                break;
                            }
                        }
                        version = file;
                        return StringUtils.isNotEmpty(version) ? version : defaultVersion;
                    }
                }
            }
            // return default version if no version info is found
            return defaultVersion;
        } catch (Throwable e) {
            // return default version when any exception is thrown
            logger.error("return default version, ignore exception " + e.getMessage(), e);
            return defaultVersion;
        }
    }

    public static void checkDuplicate(Class<?> cls, boolean failOnError) {
        checkDuplicate(cls.getName().replace('.', '/') + ".class", failOnError);
    }

    public static void checkDuplicate(Class<?> cls) {
        checkDuplicate(cls, false);
    }

    public static void checkDuplicate(String path, boolean failOnError) {
        try {
            // search in caller's classloader
            Enumeration<URL> urls = ClassHelper.getCallerClassLoader(Version.class).getResources(path);
            Set<String> files = new HashSet<String>();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url != null) {
                    String file = url.getFile();
                    if (file != null && file.length() > 0) {
                        files.add(file);
                    }
                }
            }
            // duplicated jar is found
            if (files.size() > 1) {
                String error = "Duplicate class " + path + " in " + files.size() + " jar " + files;
                if (failOnError) {
                    throw new IllegalStateException(error);
                } else {
                    logger.error(error);
                }
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

}