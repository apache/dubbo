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
package org.apache.dubbo.metadata.definition.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * 2015/1/27.
 */
public class JaketConfigurationUtils {

    private static final String CONFIGURATION_FILE = "jaket.properties";

    private static String[] includedInterfacePackages;
    private static String[] includedTypePackages;
    private static String[] closedTypes;

    static {
        Properties props = new Properties();
        InputStream inStream = JaketConfigurationUtils.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE);
        try {
            props.load(inStream);
            String value = (String) props.get("included_interface_packages");
            if (value != null && !value.isEmpty()) {
                includedInterfacePackages = value.split(",");
            }

            value = props.getProperty("included_type_packages");
            if (value != null && !value.isEmpty()) {
                includedTypePackages = value.split(",");
            }

            value = props.getProperty("closed_types");
            if (value != null && !value.isEmpty()) {
                closedTypes = value.split(",");
            }

        } catch (Throwable e) {
            // Ignore it.
        }
    }

    public static boolean isExcludedInterface(Class<?> clazz) {
        if (includedInterfacePackages == null || includedInterfacePackages.length == 0) {
            return false;
        }

        for (String packagePrefix : includedInterfacePackages) {
            if (clazz.getCanonicalName().startsWith(packagePrefix)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isExcludedType(Class<?> clazz) {
        if (includedTypePackages == null || includedTypePackages.length == 0) {
            return false;
        }

        for (String packagePrefix : includedTypePackages) {
            if (clazz.getCanonicalName().startsWith(packagePrefix)) {
                return false;
            }
        }

        return true;
    }

    public static boolean needAnalyzing(Class<?> clazz) {
        String canonicalName = clazz.getCanonicalName();

        if (closedTypes != null && closedTypes.length > 0) {
            for (String type : closedTypes) {
                if (canonicalName.startsWith(type)) {
                    return false;
                }
            }
        }

        return !isExcludedType(clazz);
    }

}
