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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
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

    private static final int MAX_LENGTH = 100;

    private static final int MAX_PATH_LENGTH = 200;

    private static final Pattern PATTERN_PATH = Pattern.compile("[\\-._/0-9a-zA-Z]+");

    private static final Pattern PATTERN_MULTI_NAME = Pattern.compile("[\\-._,0-9a-zA-Z]+");

    private static final Pattern PATTERN_METHOD_NAME = Pattern.compile("[a-zA-Z][0-9a-zA-Z]*");
    
    private static final Pattern PATTERN_NAME = Pattern.compile("[\\-._0-9a-zA-Z]+");
    
    private static final Pattern PATTERN_NAME_HAS_COLON= Pattern.compile("[:\\-._0-9a-zA-Z]+");
    
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
    
    private static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive() 
                || type == String.class 
                || type == Character.class
                || type == Boolean.class
                || type == Byte.class
                || type == Short.class
                || type == Integer.class 
                || type == Long.class
                || type == Float.class 
                || type == Double.class
                || type == Object.class;
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
                        && isPrimitive(method.getReturnType())) {
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
                        if (value != null) {
                            if (prefix != null && prefix.length() > 0) {
                                key = prefix + "." + key;
                            }
                            parameters.put(key, value);
                        }
                    } else {
                        String str = String.valueOf(value).trim();
                        if (value != null && str.length() > 0) {
                            if (parameter != null && parameter.escaped()) {
                                str = URL.encode(str);
                            }
                            if (parameter != null && parameter.append()) {
                                String pre = (String)parameters.get(Constants.DEFAULT_KEY + "." + key);
                                if (pre != null && pre.length() > 0) {
                                    str = pre + "," + str;
                                }
                                pre = (String)parameters.get(key);
                                if (pre != null && pre.length() > 0) {
                                    str = pre + "," + str;
                                }
                            }
                            if (prefix != null && prefix.length() > 0) {
                                key = prefix + "." + key;
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
    
    @Override
    public String toString() {
        try {
            String tag = getClass().getSimpleName();
            if (tag.endsWith("Config")) {
                tag = tag.substring(0, tag.length() - "Config".length());
            } else if (tag.endsWith("Bean")) {
                tag = tag.substring(0, tag.length() - "Bean".length());
            }
            tag = tag.toLowerCase();
            StringBuilder buf = new StringBuilder();
            buf.append("<dubbo:");
            buf.append(tag);
            Method[] methods = getClass().getMethods();
            for (Method method : methods) {
                try {
                    String name = method.getName();
                    if ((name.startsWith("get") || name.startsWith("is")) 
                            && ! "getClass".equals(name)
                            && Modifier.isPublic(method.getModifiers()) 
                            && method.getParameterTypes().length == 0
                            && isPrimitive(method.getReturnType())) {
                        int i = name.startsWith("get") ? 3 : 2;
                        String key = name.substring(i, i + 1).toLowerCase() + name.substring(i + 1);
                        Object value = method.invoke(this, new Object[0]);
                        if (value != null) {
                            buf.append(" ");
                            buf.append(key);
                            buf.append("=\"");
                            buf.append(value);
                            buf.append("\"");
                        }
                    }
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
            }
            buf.append(" />");
            return buf.toString();
        } catch (Throwable t) { // 防御性容错
            return super.toString();
        }
    }

}