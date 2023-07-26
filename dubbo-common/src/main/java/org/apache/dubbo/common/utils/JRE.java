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

package org.apache.dubbo.common.utils;


import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;


/**
 * JRE version
 */
public enum JRE {

    JAVA_8,

    JAVA_9,

    JAVA_10,

    JAVA_11,

    JAVA_12,

    JAVA_13,

    JAVA_14,

    JAVA_15,

    JAVA_16,

    JAVA_17,

    JAVA_18,

    JAVA_19,

    OTHER;

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(JRE.class);

    private static final JRE VERSION = getJre();

    /**
     * get current JRE version
     *
     * @return JRE version
     */
    public static JRE currentVersion() {
        return VERSION;
    }

    /**
     * is current version
     *
     * @return true if current version
     */
    public boolean isCurrentVersion() {
        return this == VERSION;
    }

    private static JRE getJre() {
        // get java version from system property
        String version = System.getProperty("java.version");
        boolean isBlank = StringUtils.isBlank(version);
        if (isBlank) {
            logger.debug("java.version is blank");
        }
        // if start with 1.8 return java 8
        if (!isBlank && version.startsWith("1.8")) {
            return JAVA_8;
        }
        // if JRE version is 9 or above, we can get version from java.lang.Runtime.version()
        try {
            Object javaRunTimeVersion = MethodUtils.invokeMethod(Runtime.getRuntime(), "version");
            int majorVersion = MethodUtils.invokeMethod(javaRunTimeVersion, "major");
            switch (majorVersion) {
                case 9:
                    return JAVA_9;
                case 10:
                    return JAVA_10;
                case 11:
                    return JAVA_11;
                case 12:
                    return JAVA_12;
                case 13:
                    return JAVA_13;
                case 14:
                    return JAVA_14;
                case 15:
                    return JAVA_15;
                case 16:
                    return JAVA_16;
                case 17:
                    return JAVA_17;
                case 18:
                    return JAVA_18;
                case 19:
                    return JAVA_19;
                default:
                    return OTHER;
            }
        } catch (Exception e) {
            logger.debug("Can't determine current JRE version (maybe java.version is null), assuming that JRE version is 8.", e);
        }
        // default java 8
        return JAVA_8;
    }

}
