/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

/**
 * AbstractConfig
 * 
 * @author william.liangf
 */
public abstract class AbstractConfig implements Serializable {

    private static final long serialVersionUID = 4267533505537413570L;

    protected static final Logger logger = LoggerFactory.getLogger(AbstractConfig.class);

    private static final Properties PROPERTIES = loadProperties();

    private static final int MAX_LENGTH = 100;

    private static final int MAX_PATH_LENGTH = 200;

    private static final Pattern PATTERN_PATH = Pattern.compile("[\\-._/0-9a-zA-Z]+");

    private static final Pattern PATTERN_MULTI_NAME = Pattern.compile("[\\-._,0-9a-zA-Z]+");

    private static final Pattern PATTERN_METHOD_NAME = Pattern.compile("[a-zA-Z][0-9a-zA-Z]*");
    
    private static final Pattern PATTERN_NAME = Pattern.compile("[\\-._0-9a-zA-Z]+");
    
    private static final Pattern PATTERN_NAME_HAS_COLON= Pattern.compile("[:\\-._0-9a-zA-Z]+");
    
    protected static String getLegacyProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.length() == 0) {
            value = PROPERTIES.getProperty(key);
        }
        return value;
    }
    
    public static void mergeProperties(Properties properties) {
        if (properties != null) {
            PROPERTIES.putAll(properties);
        }
    }
    
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try {
            InputStream input = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("dubbo.properties");
            if (input != null) {
                try {
                    properties.load(input);
                } finally {
                    input.close();
                }
            }
        } catch (Throwable e) {
            logger.warn("Fail to load dubbo.properties file: " + e.getMessage(), e);
        }
        return properties;
    }
    
    protected static void appendParameters(Map<String, String> parameters, Object config) {
        appendParameters(parameters, config, null);
    }
    
    protected static void appendParameters(Map<String, String> parameters, Object config, String prefix) {
        appendMaps(parameters, config, prefix, false);
    }
    
    protected static void appendAttributes(Map<Object, Object> parameters, Object config) {
        appendAttributes(parameters, config, null);
    }
    
    protected static void appendAttributes(Map<Object, Object> parameters, Object config, String prefix) {
        appendMaps(parameters, config, prefix, true);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void appendMaps(Map parameters, Object config, String prefix, boolean attribute) {
        if (config == null) {
            return;
        }
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                String name = method.getName();
                if ((name.startsWith("get") || name.startsWith("is")) 
                        && ! "getClass".equals(name)
                        && Modifier.isPublic(method.getModifiers()) 
                        && method.getParameterTypes().length == 0
                        && (method.getReturnType() == String.class 
                                || method.getReturnType() == Character.class
                                || method.getReturnType() == Boolean.class
                                || method.getReturnType() == Byte.class
                                || method.getReturnType() == Short.class
                                || method.getReturnType() == Integer.class 
                                || method.getReturnType() == Long.class
                                || method.getReturnType() == Float.class 
                                || method.getReturnType() == Double.class
                                || method.getReturnType() == Object.class)) {
                    Parameter parameter = method.getAnnotation(Parameter.class);
                    if (attribute){
                        if (parameter == null || !parameter.attribute())
                            continue;
                    } else {
                        if (method.getReturnType() == Object.class || parameter != null && parameter.excluded()) {
                            continue;
                        }
                    }
                    String key;
                    if (parameter != null && parameter.key() != null && parameter.key().length() > 0) {
                        key = parameter.key();
                    } else {
                        int i = name.startsWith("get") ? 3 : 2;
                        key = name.substring(i, i + 1).toLowerCase() + name.substring(i + 1);
                    }
                    Object value = method.invoke(config, new Object[0]);
                    if (attribute){
                        if (prefix != null && prefix.length() > 0) {
                            key = prefix + "." + key;
                        }
                        if (value != null) parameters.put(key, value);
                    } else {
                        String str = String.valueOf(value).trim();
                        if (value != null && str.length() > 0) {
                            if (prefix != null && prefix.length() > 0) {
                                key = prefix + "." + key;
                            }
                            if (parameter != null && parameter.escaped()) {
                                str = URLEncoder.encode(str, "UTF-8");
                            }
                            parameters.put(key, str);
                        } else if (parameter != null && parameter.required()) {
                            throw new IllegalStateException(config.getClass().getSimpleName() + "." + key + " == null");
                        }
                    }
                } else if (!attribute && "getParameters".equals(name)
                        && Modifier.isPublic(method.getModifiers()) 
                        && method.getParameterTypes().length == 0
                        && method.getReturnType() == Map.class) {
                    Map<String, String> map = (Map<String, String>) method.invoke(config, new Object[0]);
                    if (map != null && map.size() > 0) {
                        if (prefix != null && prefix.length() > 0) {
                            for (Map.Entry<String, String> entry : map.entrySet()) {
                                parameters.put(prefix + "." + entry.getKey(), entry.getValue());
                            }
                        } else {
                            parameters.putAll(map);
                        }
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }
    
    protected static void checkExtension(Class<?> type, String property, String value) {
        checkName(property, value);
        if (value != null && value.length() > 0 
                && ! ExtensionLoader.getExtensionLoader(type).hasExtension(value)) {
            throw new IllegalStateException("No such extension " + value + " for " + property + "/" + type.getName());
        }
    }
    
    protected static void checkMultiExtension(Class<?> type, String property, String value) {
        checkMultiName(property, value);
        if (value != null && value.length() > 0) {
            String[] values = value.split("\\s*[,]+\\s*");
            for (String v : values) {
                if (v.startsWith(Constants.REMOVE_VALUE_PREFIX)) {
                    v = v.substring(1);
                }
                if (! ExtensionLoader.getExtensionLoader(type).hasExtension(v)) {
                    throw new IllegalStateException("No such extension " + v + " for " + property + "/" + type.getName());
                }
            }
        }
    }

    protected static void checkLength(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, null);
    }

    protected static void checkPathLength(String property, String value) {
        checkProperty(property, value, MAX_PATH_LENGTH, null);
    }

    protected static void checkName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME);
    }
    
    protected static void checkNameHasColon(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME_HAS_COLON);
    }
    
    protected static void checkMultiName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_MULTI_NAME);
    }

    protected static void checkPathName(String property, String value) {
        checkProperty(property, value, MAX_PATH_LENGTH, PATTERN_PATH);
    }

    protected static void checkMethodName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_METHOD_NAME);
    }
    
    protected static void checkParameterName(Map<String, String> parameters) {
        if (parameters == null || parameters.size() == 0) {
            return;
        }
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            //change by tony.chenl parameter value maybe has colon.for example napoli address
            checkNameHasColon(entry.getKey(), entry.getValue());
        }
    }
    
    protected static void checkProperty(String property, String value, int maxlength, Pattern pattern) {
        if (value == null || value.length() == 0) {
            return;
        }
        if(value.length() > maxlength){
            throw new IllegalStateException("Invalid " + property + "=\"" + value + "\" is longer than " + maxlength);
        }
        if (pattern != null) {
            Matcher matcher = pattern.matcher(value);
            if(! matcher.matches()) {
                throw new IllegalStateException("Invalid " + property + "=\"" + value + "\" contain illegal charactor, only digit, letter, '-', '_' and '.' is legal.");
            }
        }
    }
    
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if (logger.isInfoEnabled()) {
                    logger.info("Run shutdown hook now.");
                }
                ProtocolConfig.destroyAll();
            }
        }, "DubboShutdownHook"));
    }

}