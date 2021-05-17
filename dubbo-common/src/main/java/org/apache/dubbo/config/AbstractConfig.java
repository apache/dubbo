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
package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.InmemoryConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.MethodUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.AsyncMethodInfo;

import javax.annotation.PostConstruct;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.utils.ReflectUtils.findMethodByMethodSignature;

/**
 * Utility methods and public methods for parsing configuration
 *
 * @export
 */
public abstract class AbstractConfig implements Serializable {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractConfig.class);
    private static final long serialVersionUID = 4267533505537413570L;

    /**
     * The legacy properties container
     */
    private static final Map<String, String> LEGACY_PROPERTIES = new HashMap<String, String>();

    /**
     * The field names cache of config class
     */
    private static final Map<Class, Set<String>> fieldNamesCache = new ConcurrentHashMap<>();

    /**
     * The ignored attributes of metadata and override config.
     * Attributes that cannot be overridden.
     */
    private static final Set<String> IGNORED_ATTRIBUTES = new HashSet<>();

    /**
     * The suffix container
     */
    private static final String[] SUFFIXES = new String[]{"Config", "Bean", "ConfigBase"};

    static {
        LEGACY_PROPERTIES.put("dubbo.protocol.name", "dubbo.service.protocol");
        LEGACY_PROPERTIES.put("dubbo.protocol.host", "dubbo.service.server.host");
        LEGACY_PROPERTIES.put("dubbo.protocol.port", "dubbo.service.server.port");
        LEGACY_PROPERTIES.put("dubbo.protocol.threads", "dubbo.service.max.thread.pool.size");
        LEGACY_PROPERTIES.put("dubbo.consumer.timeout", "dubbo.service.invoke.timeout");
        LEGACY_PROPERTIES.put("dubbo.consumer.retries", "dubbo.service.max.retry.providers");
        LEGACY_PROPERTIES.put("dubbo.consumer.check", "dubbo.service.allow.no.provider");
        LEGACY_PROPERTIES.put("dubbo.service.url", "dubbo.service.address");

        // The ignored attributes of metadata
        //IGNORED_ATTRIBUTES.add("id");
        IGNORED_ATTRIBUTES.add("prefix");
        IGNORED_ATTRIBUTES.add("prefixes");
        IGNORED_ATTRIBUTES.add("refreshed");
        IGNORED_ATTRIBUTES.add("valid");
        IGNORED_ATTRIBUTES.add("inited");
        IGNORED_ATTRIBUTES.add("parent");
    }

    /**
     * The config id
     */
    private String id;

    protected final AtomicBoolean refreshed = new AtomicBoolean(false);

    public static String getTagName(Class<?> cls) {
        String tag = cls.getSimpleName();
        for (String suffix : SUFFIXES) {
            if (tag.endsWith(suffix)) {
                tag = tag.substring(0, tag.length() - suffix.length());
                break;
            }
        }
        return StringUtils.camelToSplitName(tag, "-");
    }

    public static String getPluralTagName(Class<?> cls) {
        String tagName = getTagName(cls);
        if (tagName.endsWith("y")) {
            // e.g. registry -> registries
            return tagName.substring(0, tagName.length() - 1) + "ies";
        } else if (tagName.endsWith("s")) {
            // e.g. metrics -> metricses
            return tagName + "es";
        }
        return tagName + "s";
    }

    public static void appendParameters(Map<String, String> parameters, Object config) {
        appendParameters(parameters, config, null);
    }

    @SuppressWarnings("unchecked")
    public static void appendParameters(Map<String, String> parameters, Object config, String prefix) {
        appendParamters0(parameters, config, prefix, false);
    }

    public static void appendProperties(Map<String, String> parameters, Object config) {
        appendParamters0(parameters, config, null, true);
    }

    private static void appendParamters0(Map<String, String> parameters, Object config, String prefix, boolean appendProperty) {
        if (config == null) {
            return;
        }
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                String name = method.getName();
                if (MethodUtils.isGetter(method)) {
                    Parameter parameter = method.getAnnotation(Parameter.class);
                    if (method.getReturnType() == Object.class || parameter != null && parameter.excluded()) {
                        continue;
                    }
                    String key;
                    if (!appendProperty && parameter != null && parameter.key().length() > 0) {
                        key = parameter.key();
                    } else {
                        key = calculatePropertyFromGetter(name);
                    }
                    Object value = method.invoke(config);
                    String str = String.valueOf(value).trim();
                    if (value != null && str.length() > 0) {
                        if (!appendProperty && parameter != null && parameter.escaped()) {
                            str = URL.encode(str);
                        }
                        if (parameter != null && parameter.append()) {
                            String pre = parameters.get(key);
                            if (pre != null && pre.length() > 0) {
                                str = pre + "," + str;
                                //Remove duplicate values
                                Set<String> set = StringUtils.splitToSet(str, ',');
                                str = StringUtils.join(set, ",");
                            }
                        }
                        if (prefix != null && prefix.length() > 0) {
                            key = prefix + "." + key;
                        }
                        parameters.put(key, str);
                    } else if (!appendProperty && parameter != null && parameter.required()) {
                        throw new IllegalStateException(config.getClass().getSimpleName() + "." + key + " == null");
                    }
                } else if (isParametersGetter(method)) {
                    Map<String, String> map = (Map<String, String>) method.invoke(config, new Object[0]);
                    parameters.putAll(convert(map, prefix));
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    /**
     * Put attributes of specify 'config' into 'parameters' argument
     * @param parameters
     * @param config
     */
    @Deprecated
    protected static void appendAttributes(Map<String, Object> parameters, Object config) {
        appendAttributes(parameters, config, null);
    }

    @Deprecated
    protected static void appendAttributes(Map<String, Object> parameters, Object config, String prefix) {
        if (config == null) {
            return;
        }
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                Parameter parameter = method.getAnnotation(Parameter.class);
                if (parameter == null || !parameter.attribute()) {
                    continue;
                }
                String name = method.getName();
                if (MethodUtils.isGetter(method)) {
                    String key;
                    if (parameter.key().length() > 0) {
                        key = parameter.key();
                    } else {
                        key = calculateAttributeFromGetter(name);
                    }
                    Object value = method.invoke(config);
                    if (value != null) {
                        if (prefix != null && prefix.length() > 0) {
                            key = prefix + "." + key;
                        }
                        parameters.put(key, value);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    protected static AsyncMethodInfo convertMethodConfig2AsyncInfo(MethodConfig methodConfig) {
        if (methodConfig == null || (methodConfig.getOninvoke() == null && methodConfig.getOnreturn() == null && methodConfig.getOnthrow() == null)) {
            return null;
        }

        //check config conflict
        if (Boolean.FALSE.equals(methodConfig.isReturn()) && (methodConfig.getOnreturn() != null || methodConfig.getOnthrow() != null)) {
            throw new IllegalStateException("method config error : return attribute must be set true when onreturn or onthrow has been set.");
        }

        AsyncMethodInfo asyncMethodInfo = new AsyncMethodInfo();

        asyncMethodInfo.setOninvokeInstance(methodConfig.getOninvoke());
        asyncMethodInfo.setOnreturnInstance(methodConfig.getOnreturn());
        asyncMethodInfo.setOnthrowInstance(methodConfig.getOnthrow());

        try {
            String oninvokeMethod = methodConfig.getOninvokeMethod();
            if (StringUtils.isNotEmpty(oninvokeMethod)) {
                asyncMethodInfo.setOninvokeMethod(getMethodByName(methodConfig.getOninvoke().getClass(), oninvokeMethod));
            }

            String onreturnMethod = methodConfig.getOnreturnMethod();
            if (StringUtils.isNotEmpty(onreturnMethod)) {
                asyncMethodInfo.setOnreturnMethod(getMethodByName(methodConfig.getOnreturn().getClass(), onreturnMethod));
            }

            String onthrowMethod = methodConfig.getOnthrowMethod();
            if (StringUtils.isNotEmpty(onthrowMethod)) {
                asyncMethodInfo.setOnthrowMethod(getMethodByName(methodConfig.getOnthrow().getClass(), onthrowMethod));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        return asyncMethodInfo;
    }

    private static Method getMethodByName(Class<?> clazz, String methodName) {
        try {
            return ReflectUtils.findMethodByMethodName(clazz, methodName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected static Set<String> getSubProperties(Map<String, String> properties, String prefix) {
        return properties.keySet().stream().filter(k -> k.contains(prefix)).map(k -> {
            k = k.substring(prefix.length());
            return k.substring(0, k.indexOf("."));
        }).collect(Collectors.toSet());
    }

    private static String extractPropertyName(Class<?> clazz, Method setter) throws Exception {
        String propertyName = setter.getName().substring("set".length());
        Method getter = null;
        try {
            getter = clazz.getMethod("get" + propertyName);
        } catch (NoSuchMethodException e) {
            getter = clazz.getMethod("is" + propertyName);
        }
//        Parameter parameter = getter.getAnnotation(Parameter.class);
//        if (parameter != null && StringUtils.isNotEmpty(parameter.key()) && parameter.useKeyAsProperty()) {
//            propertyName = parameter.key();
//        } else {
//            propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
//        }

        propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
        return propertyName;
    }

    private static String calculatePropertyFromGetter(String name) {
        int i = name.startsWith("get") ? 3 : 2;
        return StringUtils.camelToSplitName(name.substring(i, i + 1).toLowerCase() + name.substring(i + 1), ".");
    }

    private static String calculateAttributeFromGetter(String getter) {
        int i = getter.startsWith("get") ? 3 : 2;
        return getter.substring(i, i + 1).toLowerCase() + getter.substring(i + 1);
    }

    private static void invokeSetParameters(Class c, Object o, Map map) {
        try {
            Method method = findMethodByMethodSignature(c, "setParameters", new String[]{Map.class.getName()});
            if (method != null && isParametersSetter(method)) {
                method.invoke(o, map);
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    private static Map<String, String> invokeGetParameters(Class c, Object o) {
        try {
            Method method = findMethodByMethodSignature(c, "getParameters", null);
            if (method != null && isParametersGetter(method)) {
                return (Map<String, String>) method.invoke(o);
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    private static boolean isParametersGetter(Method method) {
        String name = method.getName();
        return ("getParameters".equals(name)
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterTypes().length == 0
                && method.getReturnType() == Map.class);
    }

    private static boolean isParametersSetter(Method method) {
        return ("setParameters".equals(method.getName())
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterCount() == 1
                && Map.class == method.getParameterTypes()[0]
                && method.getReturnType() == void.class);
    }

    /**
     * @param parameters the raw parameters
     * @param prefix     the prefix
     * @return the parameters whose raw key will replace "-" to "."
     * @revised 2.7.8 "private" to be "protected"
     */
    protected static Map<String, String> convert(Map<String, String> parameters, String prefix) {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();
        String pre = (prefix != null && prefix.length() > 0 ? prefix + "." : "");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            result.put(pre + key, value);
            // For compatibility, key like "registry-type" will has a duplicate key "registry.type"
            if (Arrays.binarySearch(Constants.DOT_COMPATIBLE_KEYS, key) != -1) {
                result.put(pre + key.replace('-', '.'), value);
            }
        }
        return result;
    }

    @Parameter(excluded = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void updateIdIfAbsent(String value) {
        if (StringUtils.isNotEmpty(value) && StringUtils.isEmpty(id)) {
            this.id = value;
        }
    }

    /**
     * Copy attributes from annotation
     * @param annotationClass
     * @param annotation
     */
    protected void appendAnnotation(Class<?> annotationClass, Object annotation) {
        Method[] methods = annotationClass.getMethods();
        for (Method method : methods) {
            if (method.getDeclaringClass() != Object.class
                    && method.getReturnType() != void.class
                    && method.getParameterTypes().length == 0
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    String property = method.getName();
                    if ("interfaceClass".equals(property) || "interfaceName".equals(property)) {
                        property = "interface";
                    }
                    String setter = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
                    Object value = method.invoke(annotation);
                    if (value != null && !value.equals(method.getDefaultValue())) {
                        Class<?> parameterType = ReflectUtils.getBoxedClass(method.getReturnType());
                        if ("filter".equals(property) || "listener".equals(property)) {
                            parameterType = String.class;
                            value = StringUtils.join((String[]) value, ",");
                        } else if ("parameters".equals(property)) {
                            parameterType = Map.class;
                            value = CollectionUtils.toStringMap((String[]) value);
                        }
                        try {
                            Method setterMethod = getClass().getMethod(setter, parameterType);
                            setterMethod.invoke(this, value);
                        } catch (NoSuchMethodException e) {
                            // ignore
                        }
                    }
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     *
     * <p>
     * <b>The new instance of the AbstractConfig subclass should return empty metadata.</b>
     * The purpose is is to get the attributes set by the user instead of the default value when the {@link #refresh()} method handles attribute overrides.
     * </p>
     *
     * <p><b>The default value of the field should be set in the {@link #checkDefault()} method</b>,
     * which will be called at the end of {@link #refresh()}, so that it will not affect the behavior of attribute overrides.</p>
     *
     * <p></p>
     * Should be called after Config was fully initialized.
     * // FIXME: this method should be completely replaced by appendParameters?
     * // -- Not complete matched. Url parameter may use key, but props override only use property name
     * <p>
     * Notice! This method should include all properties in the returning map, treat @Parameter differently compared to appendParameters?
     * </p>
     *
     * @see AbstractConfig#checkDefault()
     * @see AbstractConfig#appendParameters(Map, Object, String)
     */
    public Map<String, String> getMetaData() {
        BeanInfo beanInfo = getBeanInfo();

        Map<String, String> metaData = new HashMap<>();
        for (java.beans.MethodDescriptor methodDescriptor : beanInfo.getMethodDescriptors()) {
            try {
                Method method = methodDescriptor.getMethod();
                String name = method.getName();
                if (MethodUtils.isMetaMethod(method)) {
                    String key;
                    Parameter parameter = method.getAnnotation(Parameter.class);
                    if (parameter != null && parameter.key().length() > 0 && parameter.useKeyAsProperty()) {
                        key = parameter.key();
                    } else {
                        key = calculateAttributeFromGetter(name);
                    }

                    // filter ignored attribute
                    if (IGNORED_ATTRIBUTES.contains(key)) {
                        continue;
                    }

                    // filter non-property
                    if (!isWritableProperty(beanInfo, key)) {
                        continue;
                    }

                    // treat url and configuration differently, the value should always present in configuration though it may not need to present in url.
                    //if (method.getReturnType() == Object.class || parameter != null && parameter.excluded()) {
                    if (method.getReturnType() == Object.class) {
                        //metaData.put(key, null);
                        continue;
                    }

                    /**
                     * Attributes annotated as deprecated should not override newly added replacement.
                     */
                    if (MethodUtils.isDeprecated(method) && metaData.get(key) != null) {
                        continue;
                    }

                    Object value = method.invoke(this);
                    String str = String.valueOf(value).trim();
                    if (value != null && str.length() > 0) {
                        metaData.put(key, str);
                    } else {
                        //metaData.put(key, null);
                    }
                } else if (isParametersGetter(method)) {
                    Map<String, String> map = (Map<String, String>) method.invoke(this, new Object[0]);
                    metaData.putAll(convert(map, ""));
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return metaData;
    }

    protected BeanInfo getBeanInfo() {
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(this.getClass());
        } catch (IntrospectionException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return beanInfo;
    }

    private static boolean isWritableProperty(BeanInfo beanInfo, String key) {
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if (key.equals(propertyDescriptor.getName())) {
                return propertyDescriptor.getWriteMethod() != null;
            }
        }
        return false;
    }

    @Parameter(excluded = true)
    public List<String> getPrefixes() {
        List<String> prefixes = new ArrayList<>();
        if (StringUtils.hasText(this.getId())) {
            // dubbo.{tag-name}s.id
            prefixes.add(CommonConstants.DUBBO + "." + getPluralTagName(this.getClass()) + "." + this.getId());
        }

        // check name
        BeanInfo beanInfo = getBeanInfo();
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if ("name".equals(propertyDescriptor.getName())) {
                try {
                    String name = (String) propertyDescriptor.getReadMethod().invoke(this);
                    if (StringUtils.hasText(name)) {
                        String prefix = CommonConstants.DUBBO + "." + getPluralTagName(this.getClass()) + "." + name;
                        if (!prefixes.contains(prefix)) {
                            prefixes.add(prefix);
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("get config name failed: "+this, e);
                }
            }
        }

        // dubbo.{tag-name}
        prefixes.add(getTypePrefix(this.getClass()));
        return prefixes;
    }

    public static String getTypePrefix(Class<? extends AbstractConfig> cls) {
        return CommonConstants.DUBBO + "." + getTagName(cls);
    }

    /**
     * Dubbo config property override
     */
    public void refresh() {
        refreshed.set(true);
        try {
            preProcessRefresh();

            Configuration instanceConfiguration = null;
            Environment env = ApplicationModel.getEnvironment();
            List<Map<String, Object>> configurationMaps = env.getConfigurationMaps();

            // Check in order to see if there are any properties that begin with PREFIX
            String preferredPrefix = null;
            for (String prefix : getPrefixes()) {
                if (ConfigurationUtils.hasSubProperties(configurationMaps, prefix)) {
                    preferredPrefix = prefix;
                    break;
                }
            }
            if (preferredPrefix == null) {
                preferredPrefix = getPrefixes().get(0);
            }
            Collection<Map<String, Object>> instanceConfigMaps = env.getConfigurationMaps(this, preferredPrefix);
            Map<String, Object> subProperties = ConfigurationUtils.getSubProperties(instanceConfigMaps, preferredPrefix);
            instanceConfiguration = new InmemoryConfiguration(subProperties);

            // loop methods, get override value and set the new value back to method
            Method[] methods = getClass().getMethods();
            for (Method method : methods) {
                if (MethodUtils.isSetter(method)) {
                    String propertyName = extractPropertyName(getClass(), method);
                    // convert camelCase/snake_case to kebab-case
                    propertyName = StringUtils.convertToSplitName(propertyName, "-");

                    // check ignored attributes
                    if (IGNORED_ATTRIBUTES.contains(propertyName)) {
                        continue;
                    }
                    try {
                        String value = StringUtils.trim(instanceConfiguration.getString(propertyName));
                        // isTypeMatch() is called to avoid duplicate and incorrect update, for example, we have two 'setGeneric' methods in ReferenceConfig.
                        if (StringUtils.isNotEmpty(value) && ClassUtils.isTypeMatch(method.getParameterTypes()[0], value)) {
                            method.invoke(this, ClassUtils.convertPrimitive(method.getParameterTypes()[0], value));
                        }
                    } catch (Exception e) {
                        logger.info("Failed to override the property " + method.getName() + " in " +
                                this.getClass().getSimpleName() +
                                ", please make sure every property has getter/setter method provided.");
                    }
                } else if (isParametersSetter(method)) {
                    String propertyName = extractPropertyName(getClass(), method);
                    String value = StringUtils.trim(instanceConfiguration.getString(propertyName));
                    if (StringUtils.isNotEmpty(value)) {
                        Map<String, String> map = invokeGetParameters(getClass(), this);
                        map = map == null ? new HashMap<>() : map;
                        map.putAll(convert(StringUtils.parseParameters(value), ""));
                        invokeSetParameters(getClass(), this, map);
                    }
                }
            }

            // refresh methodConfig
            if (this instanceof AbstractInterfaceConfig) {
                AbstractInterfaceConfig interfaceConfig = (AbstractInterfaceConfig) this;
                List<MethodConfig> methodConfigs = interfaceConfig.getMethods();
                if (methodConfigs != null && methodConfigs.size() > 0) {
                    for (MethodConfig methodConfig : methodConfigs) {
                        methodConfig.setParent(interfaceConfig);
                        methodConfig.refresh();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to override field value of config bean: "+this, e);
            throw new IllegalStateException("Failed to override field value of config bean: "+this, e);
        }

        postProcessRefresh();
    }

    protected void preProcessRefresh() {
        // pre-process refresh
    }

    protected void postProcessRefresh() {
        // post-process refresh
        checkDefault();
    }

    /**
     * Check and set default value for some fields.
     * <p>
     * This method will be called at the end of {@link #refresh()}, as a post-initializer.
     * </p>
     * <p>NOTE: </p>
     * <p>
     * To distinguish between user-set property values and default property values,
     * do not initialize default value at field declare statement. <b>If the field has a default value,
     * it should be set in the checkDefault() method</b>, which will be called at the end of {@link #refresh()},
     * so that it will not affect the behavior of attribute overrides.</p>
     *
     * @see AbstractConfig#getMetaData()
     * @see AbstractConfig#appendProperties(Map, Object)
     */
    protected void checkDefault() {
    }

    @Parameter(excluded = true)
    public boolean isRefreshed() {
        return refreshed.get();
    }

    @Override
    public String toString() {
        try {

            Set<String> fieldNames = getFieldNames(this.getClass());

            StringBuilder buf = new StringBuilder();
            buf.append("<dubbo:");
            buf.append(getTagName(getClass()));
            Method[] methods = getClass().getMethods();
            for (Method method : methods) {
                try {
                    if (MethodUtils.isGetter(method)) {
                        String name = method.getName();
                        String key = calculateAttributeFromGetter(name);

                        // Fixes #4992, endless recursive call when NetUtils method fails.
                        if (!fieldNames.contains(key)) {
                            continue;
                        }

                        Object value = method.invoke(this);
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
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
            return super.toString();
        }
    }

    private static Set<String> getFieldNames(Class<?> configClass) {
        return fieldNamesCache.computeIfAbsent(configClass, ReflectUtils::getAllFieldNames);
    }

    /**
     * FIXME check @Parameter(required=true) and any conditions that need to match.
     */
    @Parameter(excluded = true)
    public boolean isValid() {
        return true;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj.getClass().getName().equals(this.getClass().getName()))) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        BeanInfo beanInfo = getBeanInfo();
        for (MethodDescriptor methodDescriptor : beanInfo.getMethodDescriptors()) {
            Method method1 = methodDescriptor.getMethod();
            if (MethodUtils.isGetter(method1)) {
                // Some properties may be excluded in url parameters, but it is meaningful and cannot be ignored. e.g., ProtocolConfig's name & port
//                Parameter parameter = method1.getAnnotation(Parameter.class);
//                if (parameter != null && parameter.excluded()) {
//                    continue;
//                }
                String propertyName = calculateAttributeFromGetter(method1.getName());
                // filter ignored attribute
                if (IGNORED_ATTRIBUTES.contains(propertyName) || "id".equals(propertyName)) {
                    continue;
                }
                // filter non-property
                if (!isWritableProperty(beanInfo, propertyName)) {
                    continue;
                }
                try {
                    Method method2 = obj.getClass().getMethod(method1.getName(), method1.getParameterTypes());
                    Object value1 = method1.invoke(this, new Object[]{});
                    Object value2 = method2.invoke(obj, new Object[]{});
                    if (!Objects.equals(value1, value2)) {
                        return false;
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("compare config instances failed", e);
                }
            }
        }
        return true;
    }

    /**
     * Add {@link AbstractConfig instance} into {@link ConfigManager}
     * <p>
     * Current method will invoked by Spring or Java EE container automatically, or should be triggered manually.
     *
     * @see ConfigManager#addConfig(AbstractConfig)
     * @since 2.7.5
     */
    @PostConstruct
    public void addIntoConfigManager() {
        ApplicationModel.getConfigManager().addConfig(this);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;

        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            if (MethodUtils.isGetter(method)) {
                Parameter parameter = method.getAnnotation(Parameter.class);
                if (parameter != null && parameter.excluded()) {
                    continue;
                }
                try {
                    Object value = method.invoke(this, new Object[]{});
                    hashCode = 31 * hashCode + value.hashCode();
                } catch (Exception ignored) {
                    //ignored
                }
            }
        }

        if (hashCode == 0) {
            hashCode = 1;
        }

        return hashCode;
    }
}
