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

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.InmemoryConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOVE_VALUE_PREFIX;

public class ConfigUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    private static Pattern VARIABLE_PATTERN = Pattern.compile(
            "\\$\\s*\\{?\\s*([\\._0-9a-zA-Z]+)\\s*\\}?");
    private static volatile Properties PROPERTIES;
    private static int PID = -1;

    private ConfigUtils() {
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    public static boolean isEmpty(String value) {
        return StringUtils.isEmpty(value)
                || "false".equalsIgnoreCase(value)
                || "0".equalsIgnoreCase(value)
                || "null".equalsIgnoreCase(value)
                || "N/A".equalsIgnoreCase(value);
    }

    public static boolean isDefault(String value) {
        return "true".equalsIgnoreCase(value)
                || "default".equalsIgnoreCase(value);
    }

    /**
     * Insert default extension into extension list.
     * <p>
     * Extension list support<ul>
     * <li>Special value <code><strong>default</strong></code>, means the location for default extensions.
     * <li>Special symbol<code><strong>-</strong></code>, means remove. <code>-foo1</code> will remove default extension 'foo'; <code>-default</code> will remove all default extensions.
     * </ul>
     *
     * @param type Extension type
     * @param cfg  Extension name list
     * @param def  Default extension list
     * @return result extension list
     */
    public static List<String> mergeValues(Class<?> type, String cfg, List<String> def) {
        List<String> defaults = new ArrayList<String>();
        if (def != null) {
            for (String name : def) {
                if (ExtensionLoader.getExtensionLoader(type).hasExtension(name)) {
                    defaults.add(name);
                }
            }
        }

        List<String> names = new ArrayList<String>();

        // add initial values
        String[] configs = (cfg == null || cfg.trim().length() == 0) ? new String[0] : COMMA_SPLIT_PATTERN.split(cfg);
        for (String config : configs) {
            if (config != null && config.trim().length() > 0) {
                names.add(config);
            }
        }

        // -default is not included
        if (!names.contains(REMOVE_VALUE_PREFIX + DEFAULT_KEY)) {
            // add default extension
            int i = names.indexOf(DEFAULT_KEY);
            if (i > 0) {
                names.addAll(i, defaults);
            } else {
                names.addAll(0, defaults);
            }
            names.remove(DEFAULT_KEY);
        } else {
            names.remove(DEFAULT_KEY);
        }

        // merge - configuration
        for (String name : new ArrayList<String>(names)) {
            if (name.startsWith(REMOVE_VALUE_PREFIX)) {
                names.remove(name);
                names.remove(name.substring(1));
            }
        }
        return names;
    }

    public static String replaceProperty(String expression, Map<String, String> params) {
        return replaceProperty(expression, new InmemoryConfiguration(params));
    }

    public static String replaceProperty(String expression, Configuration configuration) {
        if (expression == null || expression.length() == 0 || expression.indexOf('$') < 0) {
            return expression;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = System.getProperty(key);
            if (value == null && configuration != null) {
                Object val = configuration.getProperty(key);
                value = (val != null) ? val.toString() : null;
            }
            if (value == null) {
                // maybe not placeholders, use origin express
                value = matcher.group();
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Get dubbo properties.
     * It is not recommended to use this method to modify dubbo properties.
     * @return
     */
    public static Properties getProperties() {
        //TODO remove global instance PROPERTIES from ConfigUtils
        if (PROPERTIES == null) {
            synchronized (ConfigUtils.class) {
                if (PROPERTIES == null) {
                    String path = System.getProperty(CommonConstants.DUBBO_PROPERTIES_KEY);
                    if (path == null || path.length() == 0) {
                        path = System.getenv(CommonConstants.DUBBO_PROPERTIES_KEY);
                        if (path == null || path.length() == 0) {
                            path = CommonConstants.DEFAULT_DUBBO_PROPERTIES;
                        }
                    }
                    PROPERTIES = ConfigUtils.loadProperties(path, false, true);
                }
            }
        }
        return PROPERTIES;
    }

    /**
     * This method should be called before Dubbo starting.
     * It is not recommended to use this method to modify dubbo properties.
     */
    public static void setProperties(Properties properties) {
        PROPERTIES = properties;
    }

    /**
     * This method should be called before Dubbo starting.
     * It is not recommended to use this method to modify dubbo properties.
     */
    public static void addProperties(Properties properties) {
        if (properties != null) {
            getProperties().putAll(properties);
        }
    }

    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && value.length() > 0) {
            return value;
        }
        return getProperty(getProperties(), key, defaultValue);
    }

    public static String getProperty(Properties properties, String key, String defaultValue) {
        return replaceProperty(properties.getProperty(key, defaultValue), (Map) properties);
    }

    /**
     * System environment -> System properties
     *
     * @param key key
     * @return value
     */
    public static String getSystemProperty(String key) {
        String value = System.getenv(key);
        if (StringUtils.isEmpty(value)) {
            value = System.getProperty(key);
        }
        return value;
    }

    public static Properties loadProperties(String fileName) {
        return loadProperties(fileName, false, false);
    }

    public static Properties loadProperties(String fileName, boolean allowMultiFile) {
        return loadProperties(fileName, allowMultiFile, false);
    }

    /**
     * Load properties file to {@link Properties} from class path.
     *
     * @param fileName       properties file name. for example: <code>dubbo.properties</code>, <code>METE-INF/conf/foo.properties</code>
     * @param allowMultiFile if <code>false</code>, throw {@link IllegalStateException} when found multi file on the class path.
     * @param optional       is optional. if <code>false</code>, log warn when properties config file not found!s
     * @return loaded {@link Properties} content. <ul>
     * <li>return empty Properties if no file found.
     * <li>merge multi properties file if found multi file
     * </ul>
     * @throws IllegalStateException not allow multi-file, but multi-file exist on class path.
     */
    public static Properties loadProperties(String fileName, boolean allowMultiFile, boolean optional) {
        Properties properties = new Properties();
        // add scene judgement in windows environment Fix 2557
        if (checkFileNameExist(fileName)) {
            try {
                FileInputStream input = new FileInputStream(fileName);
                try {
                    properties.load(input);
                } finally {
                    input.close();
                }
            } catch (Throwable e) {
                logger.warn("Failed to load " + fileName + " file from " + fileName + "(ignore this file): " + e.getMessage(), e);
            }
            return properties;
        }

        List<java.net.URL> list = new ArrayList<java.net.URL>();
        try {
            Enumeration<java.net.URL> urls = ClassUtils.getClassLoader().getResources(fileName);
            list = new ArrayList<java.net.URL>();
            while (urls.hasMoreElements()) {
                list.add(urls.nextElement());
            }
        } catch (Throwable t) {
            logger.warn("Fail to load " + fileName + " file: " + t.getMessage(), t);
        }

        if (list.isEmpty()) {
            if (!optional) {
                logger.warn("No " + fileName + " found on the class path.");
            }
            return properties;
        }

        if (!allowMultiFile) {
            if (list.size() > 1) {
                String errMsg = String.format("only 1 %s file is expected, but %d dubbo.properties files found on class path: %s",
                        fileName, list.size(), list.toString());
                logger.warn(errMsg);
            }

            // fall back to use method getResourceAsStream
            try {
                properties.load(ClassUtils.getClassLoader().getResourceAsStream(fileName));
            } catch (Throwable e) {
                logger.warn("Failed to load " + fileName + " file from " + fileName + "(ignore this file): " + e.getMessage(), e);
            }
            return properties;
        }

        logger.info("load " + fileName + " properties file from " + list);

        for (java.net.URL url : list) {
            try {
                Properties p = new Properties();
                InputStream input = url.openStream();
                if (input != null) {
                    try {
                        p.load(input);
                        properties.putAll(p);
                    } finally {
                        try {
                            input.close();
                        } catch (Throwable t) {
                        }
                    }
                }
            } catch (Throwable e) {
                logger.warn("Fail to load " + fileName + " file from " + url + "(ignore this file): " + e.getMessage(), e);
            }
        }

        return properties;
    }

    public static String loadMigrationRule(String fileName) {
        String rawRule = "";
        if (checkFileNameExist(fileName)) {
            try {
                try (FileInputStream input = new FileInputStream(fileName)) {
                    rawRule = readString(input);
                }
            } catch (Throwable e) {
                logger.warn("Failed to load " + fileName + " file from " + fileName + "(ignore this file): " + e.getMessage(), e);
            }
            return rawRule;
        }

        try {
            InputStream is = ClassUtils.getClassLoader().getResourceAsStream(fileName);
            if (is != null) {
                rawRule = readString(is);
            }
        } catch (Throwable e) {
            logger.warn("Failed to load " + fileName + " file from " + fileName + "(ignore this file): " + e.getMessage(), e);
        }
        return rawRule;
    }

    private static String readString(InputStream is) {
        StringBuilder stringBuilder = new StringBuilder();
        char[] buffer = new char[10];
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))){
            int n;
            while ((n = reader.read(buffer)) != -1) {
                if (n < 10) {
                    buffer = Arrays.copyOf(buffer, n);
                }
                stringBuilder.append(String.valueOf(buffer));
                buffer = new char[10];
            }
        } catch (IOException e) {
            logger.error("Read migration file error.", e);
        }

        return stringBuilder.toString();
    }

    /**
     * check if the fileName can be found in filesystem
     *
     * @param fileName
     * @return
     */
    private static boolean checkFileNameExist(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }


    public static int getPid() {
        if (PID < 0) {
            try {
                RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
                // format: "pid@hostname"
                String name = runtime.getName();
                PID = Integer.parseInt(name.substring(0, name.indexOf('@')));
            } catch (Throwable e) {
                PID = 0;
            }
        }
        return PID;
    }
}
