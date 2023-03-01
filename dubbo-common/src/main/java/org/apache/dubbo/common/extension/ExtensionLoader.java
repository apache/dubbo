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
package org.apache.dubbo.common.extension;

import org.apache.dubbo.common.Extension;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beans.support.InstantiationStrategy;
import org.apache.dubbo.common.context.Lifecycle;
import org.apache.dubbo.common.extension.support.ActivateComparator;
import org.apache.dubbo.common.extension.support.WrapperComparator;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.resource.Disposable;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.ClassLoaderResourceLoader;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.NativeUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelAccessor;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOVE_VALUE_PREFIX;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_ERROR_LOAD_EXTENSION;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_LOAD_ENV_VARIABLE;

/**
 * {@link org.apache.dubbo.rpc.model.ApplicationModel}, {@code DubboBootstrap} and this class are
 * at present designed to be singleton or static (by itself totally static or uses some static fields).
 * So the instances returned from them are of process or classloader scope. If you want to support
 * multiple dubbo servers in a single process, you may need to refactor these three classes.
 * <p>
 * Load dubbo extensions
 * <ul>
 * <li>auto inject dependency extension </li>
 * <li>auto wrap extension in wrapper </li>
 * <li>default extension is an adaptive instance</li>
 * </ul>
 *
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#Service%20Provider">Service Provider in Java 5</a>
 * @see org.apache.dubbo.common.extension.SPI
 * @see org.apache.dubbo.common.extension.Adaptive
 * @see org.apache.dubbo.common.extension.Activate
 */

public class ExtensionLoader<T> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(
        ExtensionLoader.class);

    private static final Pattern NAME_SEPARATOR         = Pattern.compile("\\s*[,]+\\s*");
    private static final String  SPECIAL_SPI_PROPERTIES = "special_spi.properties";

    private final ConcurrentMap<Class<?>, Object> extensionInstances = new ConcurrentHashMap<>(64);

    private final Class<?> type;

    private final ExtensionInjector injector;

    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<>();

    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    private final    Map<String, Object>                   cachedActivates        = Collections.synchronizedMap(
        new LinkedHashMap<>());
    private final    Map<String, Set<String>>              cachedActivateGroups   = Collections.synchronizedMap(
        new LinkedHashMap<>());
    private final    Map<String, String[][]>               cachedActivateValues   = Collections.synchronizedMap(
        new LinkedHashMap<>());
    private final    ConcurrentMap<String, Holder<Object>> cachedInstances        = new ConcurrentHashMap<>();
    private final    Holder<Object>                        cachedAdaptiveInstance = new Holder<>();
    private volatile Class<?>                              cachedAdaptiveClass    = null;
    private          String                                cachedDefaultName;
    private volatile Throwable                             createAdaptiveInstanceError;

    private Set<Class<?>> cachedWrapperClasses;

    private final Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<>();

    private static volatile LoadingStrategy[] strategies = loadLoadingStrategies();

    private static final Map<String, String> specialSPILoadingStrategyMap = getSpecialSPILoadingStrategyMap();

    private static SoftReference<Map<java.net.URL, List<String>>> urlListMapCache = new SoftReference<>(
        new ConcurrentHashMap<>());

    private static final List<String> ignoredInjectMethodsDesc = getIgnoredInjectMethodsDesc();

    /**
     * Record all unacceptable exceptions when using SPI
     */
    private final Set<String>                  unacceptableExceptions = new ConcurrentHashSet<>();
    private final ExtensionDirector            extensionDirector;
    private final List<ExtensionPostProcessor> extensionPostProcessors;
    private InstantiationStrategy        instantiationStrategy;
    private final ActivateComparator           activateComparator;
    private final ScopeModel                   scopeModel;
    private final AtomicBoolean                destroyed              = new AtomicBoolean();

    public static void setLoadingStrategies(LoadingStrategy... strategies) {
        if (ArrayUtils.isNotEmpty(strategies)) {
            ExtensionLoader.strategies = strategies;
        }
    }

    /**
     * Load all {@link Prioritized prioritized} {@link LoadingStrategy Loading Strategies} via {@link ServiceLoader}
     *
     * @return non-null
     * @since 2.7.7
     */
    private static LoadingStrategy[] loadLoadingStrategies() {
        return stream(load(LoadingStrategy.class).spliterator(), false).sorted()
            .toArray(LoadingStrategy[]::new);
    }

    /**
     * some spi are implements by dubbo framework only and scan multi classloaders resources may cause
     * application startup very slow
     *
     * @return
     */
    private static Map<String, String> getSpecialSPILoadingStrategyMap() {
        Map map = new ConcurrentHashMap<>();
        Properties properties = loadProperties(ExtensionLoader.class.getClassLoader(),
            SPECIAL_SPI_PROPERTIES);
        map.putAll(properties);
        return map;
    }

    /**
     * Get all {@link LoadingStrategy Loading Strategies}
     *
     * @return non-null
     * @see LoadingStrategy
     * @see Prioritized
     * @since 2.7.7
     */
    public static List<LoadingStrategy> getLoadingStrategies() {
        return asList(strategies);
    }

    private static List<String> getIgnoredInjectMethodsDesc() {
        List<String> ignoreInjectMethodsDesc = new ArrayList<>();
        Arrays.stream(ScopeModelAware.class.getMethods()).map(ReflectUtils::getDesc)
            .forEach(ignoreInjectMethodsDesc::add);
        Arrays.stream(ExtensionAccessorAware.class.getMethods()).map(ReflectUtils::getDesc)
            .forEach(ignoreInjectMethodsDesc::add);
        return ignoreInjectMethodsDesc;
    }

    ExtensionLoader(Class<?> type, ExtensionDirector extensionDirector, ScopeModel scopeModel) {
        this.type = type;
        this.extensionDirector = extensionDirector;
        this.extensionPostProcessors = extensionDirector.getExtensionPostProcessors();
        initInstantiationStrategy();
        this.injector = (type == ExtensionInjector.class ?
            null :
            extensionDirector.getExtensionLoader(ExtensionInjector.class).getAdaptiveExtension());
        this.activateComparator = new ActivateComparator(extensionDirector);
        this.scopeModel = scopeModel;
    }

    private void initInstantiationStrategy() {
        instantiationStrategy = extensionPostProcessors.stream()
            .filter(extensionPostProcessor -> extensionPostProcessor instanceof ScopeModelAccessor)
            .map(extensionPostProcessor -> new InstantiationStrategy(
                (ScopeModelAccessor) extensionPostProcessor)).findFirst()
            .orElse(new InstantiationStrategy());
    }

    /**
     * @see ApplicationModel#getExtensionDirector()
     * @see FrameworkModel#getExtensionDirector()
     * @see ModuleModel#getExtensionDirector()
     * @see ExtensionDirector#getExtensionLoader(java.lang.Class)
     * @deprecated get extension loader from extension director of some module.
     */
    @Deprecated
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        return ApplicationModel.defaultModel().getDefaultModule().getExtensionLoader(type);
    }

    @Deprecated
    public static void resetExtensionLoader(Class type) {
    }

    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        // destroy raw extension instance
        extensionInstances.forEach((type, instance) -> {
            if (instance instanceof Disposable) {
                Disposable disposable = (Disposable) instance;
                try {
                    disposable.destroy();
                } catch (Exception e) {
                    logger.error(COMMON_ERROR_LOAD_EXTENSION, "", "",
                        "Error destroying extension " + disposable, e);
                }
            }
        });
        extensionInstances.clear();

        // destroy wrapped extension instance
        for (Holder<Object> holder : cachedInstances.values()) {
            Object wrappedInstance = holder.get();
            if (wrappedInstance instanceof Disposable) {
                Disposable disposable = (Disposable) wrappedInstance;
                try {
                    disposable.destroy();
                } catch (Exception e) {
                    logger.error(COMMON_ERROR_LOAD_EXTENSION, "", "",
                        "Error destroying extension " + disposable, e);
                }
            }
        }
        cachedInstances.clear();
    }

    private void checkDestroyed() {
        if (destroyed.get()) {
            throw new IllegalStateException("ExtensionLoader is destroyed: " + type);
        }
    }

    public String getExtensionName(T extensionInstance) {
        return getExtensionName(extensionInstance.getClass());
    }

    public String getExtensionName(Class<?> extensionClass) {
        getExtensionClasses();// load class
        return cachedNames.get(extensionClass);
    }

    /**
     * This is equivalent to {@code getActivateExtension(url, key, null)}
     *
     * @param url url
     * @param key url parameter key which used to get extension point names
     * @return extension list which are activated.
     * @see #getActivateExtension(org.apache.dubbo.common.URL, String, String)
     */
    public List<T> getActivateExtension(URL url, String key) {
        return getActivateExtension(url, key, null);
    }

    /**
     * This is equivalent to {@code getActivateExtension(url, values, null)}
     *
     * @param url    url
     * @param values extension point names
     * @return extension list which are activated
     * @see #getActivateExtension(org.apache.dubbo.common.URL, String[], String)
     */
    public List<T> getActivateExtension(URL url, String[] values) {
        return getActivateExtension(url, values, null);
    }

    /**
     * This is equivalent to {@code getActivateExtension(url, url.getParameter(key).split(","), null)}
     *
     * @param url   url
     * @param key   url parameter key which used to get extension point names
     * @param group group
     * @return extension list which are activated.
     * @see #getActivateExtension(org.apache.dubbo.common.URL, String[], String)
     */
    public List<T> getActivateExtension(URL url, String key, String group) {
        String value = url.getParameter(key);
        return getActivateExtension(url,
            StringUtils.isEmpty(value) ? null : COMMA_SPLIT_PATTERN.split(value), group);
    }

    /**
     * Get activate extensions.
     *
     * @param url    url
     * @param values extension point names
     * @param group  group
     * @return extension list which are activated
     * @see org.apache.dubbo.common.extension.Activate
     */
    @SuppressWarnings("deprecation")
    public List<T> getActivateExtension(URL url, String[] values, String group) {
        checkDestroyed();
        // solve the bug of using @SPI's wrapper method to report a null pointer exception.
        Map<Class<?>, T> activateExtensionsMap = new TreeMap<>(activateComparator);
        List<String> names = values == null ? new ArrayList<>(0) : asList(values);
        Set<String> namesSet = new HashSet<>(names);
        if (!namesSet.contains(REMOVE_VALUE_PREFIX + DEFAULT_KEY)) {
            if (cachedActivateGroups.size() == 0) {
                synchronized (cachedActivateGroups) {
                    // cache all extensions
                    if (cachedActivateGroups.size() == 0) {
                        getExtensionClasses();
                        for (Map.Entry<String, Object> entry : cachedActivates.entrySet()) {
                            String name = entry.getKey();
                            Object activate = entry.getValue();

                            String[] activateGroup, activateValue;

                            if (activate instanceof Activate) {
                                activateGroup = ((Activate) activate).group();
                                activateValue = ((Activate) activate).value();
                            } else if (activate instanceof com.alibaba.dubbo.common.extension.Activate) {
                                activateGroup = ((com.alibaba.dubbo.common.extension.Activate) activate).group();
                                activateValue = ((com.alibaba.dubbo.common.extension.Activate) activate).value();
                            } else {
                                continue;
                            }
                            cachedActivateGroups.put(name,
                                new HashSet<>(Arrays.asList(activateGroup)));
                            String[][] keyPairs = new String[activateValue.length][];
                            for (int i = 0; i < activateValue.length; i++) {
                                if (activateValue[i].contains(":")) {
                                    keyPairs[i] = new String[2];
                                    String[] arr = activateValue[i].split(":");
                                    keyPairs[i][0] = arr[0];
                                    keyPairs[i][1] = arr[1];
                                } else {
                                    keyPairs[i] = new String[1];
                                    keyPairs[i][0] = activateValue[i];
                                }
                            }
                            cachedActivateValues.put(name, keyPairs);
                        }
                    }
                }
            }

            // traverse all cached extensions
            cachedActivateGroups.forEach((name, activateGroup) -> {
                if (isMatchGroup(group, activateGroup) && !namesSet.contains(
                    name) && !namesSet.contains(REMOVE_VALUE_PREFIX + name) && isActive(
                    cachedActivateValues.get(name), url)) {

                    activateExtensionsMap.put(getExtensionClass(name), getExtension(name));
                }
            });
        }

        if (namesSet.contains(DEFAULT_KEY)) {
            // will affect order
            // `ext1,default,ext2` means ext1 will happens before all of the default extensions while ext2 will after them
            ArrayList<T> extensionsResult = new ArrayList<>(
                activateExtensionsMap.size() + names.size());
            for (String name : names) {
                if (name.startsWith(REMOVE_VALUE_PREFIX) || namesSet.contains(
                    REMOVE_VALUE_PREFIX + name)) {
                    continue;
                }
                if (DEFAULT_KEY.equals(name)) {
                    extensionsResult.addAll(activateExtensionsMap.values());
                    continue;
                }
                if (containsExtension(name)) {
                    extensionsResult.add(getExtension(name));
                }
            }
            return extensionsResult;
        } else {
            // add extensions, will be sorted by its order
            for (String name : names) {
                if (name.startsWith(REMOVE_VALUE_PREFIX) || namesSet.contains(
                    REMOVE_VALUE_PREFIX + name)) {
                    continue;
                }
                if (DEFAULT_KEY.equals(name)) {
                    continue;
                }
                if (containsExtension(name)) {
                    activateExtensionsMap.put(getExtensionClass(name), getExtension(name));
                }
            }
            return new ArrayList<>(activateExtensionsMap.values());
        }
    }

    public List<T> getActivateExtensions() {
        checkDestroyed();
        List<T> activateExtensions = new ArrayList<>();
        TreeMap<Class<?>, T> activateExtensionsMap = new TreeMap<>(activateComparator);
        getExtensionClasses();
        for (Map.Entry<String, Object> entry : cachedActivates.entrySet()) {
            String name = entry.getKey();
            Object activate = entry.getValue();
            if (!(activate instanceof Activate)) {
                continue;
            }
            activateExtensionsMap.put(getExtensionClass(name), getExtension(name));
        }
        if (!activateExtensionsMap.isEmpty()) {
            activateExtensions.addAll(activateExtensionsMap.values());
        }

        return activateExtensions;
    }

    private boolean isMatchGroup(String group, Set<String> groups) {
        if (StringUtils.isEmpty(group)) {
            return true;
        }
        if (CollectionUtils.isNotEmpty(groups)) {
            return groups.contains(group);
        }
        return false;
    }

    private boolean isActive(String[][] keyPairs, URL url) {
        if (keyPairs.length == 0) {
            return true;
        }
        for (String[] keyPair : keyPairs) {
            // @Active(value="key1:value1, key2:value2")
            String key;
            String keyValue = null;
            if (keyPair.length > 1) {
                key = keyPair[0];
                keyValue = keyPair[1];
            } else {
                key = keyPair[0];
            }

            String realValue = url.getParameter(key);
            if (StringUtils.isEmpty(realValue)) {
                realValue = url.getAnyMethodParameter(key);
            }
            if ((keyValue != null && keyValue.equals(
                realValue)) || (keyValue == null && ConfigUtils.isNotEmpty(realValue))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get extension's instance. Return <code>null</code> if extension is not found or is not initialized. Pls. note
     * that this method will not trigger extension load.
     * <p>
     * In order to trigger extension load, call {@link #getExtension(String)} instead.
     *
     * @see #getExtension(String)
     */
    @SuppressWarnings("unchecked")
    public T getLoadedExtension(String name) {
        checkDestroyed();
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        Holder<Object> holder = getOrCreateHolder(name);
        return (T) holder.get();
    }

    private Holder<Object> getOrCreateHolder(String name) {
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        return holder;
    }

    /**
     * Return the list of extensions which are already loaded.
     * <p>
     * Usually {@link #getSupportedExtensions()} should be called in order to get all extensions.
     *
     * @see #getSupportedExtensions()
     */
    public Set<String> getLoadedExtensions() {
        return Collections.unmodifiableSet(new TreeSet<>(cachedInstances.keySet()));
    }

    @SuppressWarnings("unchecked")
    public List<T> getLoadedExtensionInstances() {
        checkDestroyed();
        List<T> instances = new ArrayList<>();
        cachedInstances.values().forEach(holder -> instances.add((T) holder.get()));
        return instances;
    }

    /**
     * Find the extension with the given name.
     *
     * @throws IllegalStateException If the specified extension is not found.
     */
    public T getExtension(String name) {
        T extension = getExtension(name, true);
        if (extension == null) {
            throw new IllegalArgumentException("Not find extension: " + name);
        }
        return extension;
    }

    @SuppressWarnings("unchecked")
    public T getExtension(String name, boolean wrap) {
        checkDestroyed();
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        if ("true".equals(name)) {
            return getDefaultExtension();
        }
        String cacheKey = name;
        if (!wrap) {
            cacheKey += "_origin";
        }
        final Holder<Object> holder = getOrCreateHolder(cacheKey);
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name, wrap);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * Get the extension by specified name if found, or {@link #getDefaultExtension() returns the default one}
     *
     * @param name the name of extension
     * @return non-null
     */
    public T getOrDefaultExtension(String name) {
        return containsExtension(name) ? getExtension(name) : getDefaultExtension();
    }

    /**
     * Return default extension, return <code>null</code> if it's not configured.
     */
    public T getDefaultExtension() {
        getExtensionClasses();
        if (StringUtils.isBlank(cachedDefaultName) || "true".equals(cachedDefaultName)) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    public boolean hasExtension(String name) {
        checkDestroyed();
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        Class<?> c = this.getExtensionClass(name);
        return c != null;
    }

    public Set<String> getSupportedExtensions() {
        checkDestroyed();
        Map<String, Class<?>> classes = getExtensionClasses();
        return Collections.unmodifiableSet(new TreeSet<>(classes.keySet()));
    }

    public Set<T> getSupportedExtensionInstances() {
        checkDestroyed();
        List<T> instances = new LinkedList<>();
        Set<String> supportedExtensions = getSupportedExtensions();
        if (CollectionUtils.isNotEmpty(supportedExtensions)) {
            for (String name : supportedExtensions) {
                instances.add(getExtension(name));
            }
        }
        // sort the Prioritized instances
        instances.sort(Prioritized.COMPARATOR);
        return new LinkedHashSet<>(instances);
    }

    /**
     * Return default extension name, return <code>null</code> if not configured.
     */
    public String getDefaultExtensionName() {
        getExtensionClasses();
        return cachedDefaultName;
    }

    /**
     * Register new extension via API
     *
     * @param name  extension name
     * @param clazz extension class
     * @throws IllegalStateException when extension with the same name has already been registered.
     */
    public void addExtension(String name, Class<?> clazz) {
        checkDestroyed();
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException(
                "Input type " + clazz + " doesn't implement the Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " + clazz + " can't be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException(
                    "Extension name is blank (Extension " + type + ")!");
            }
            if (cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException(
                    "Extension name " + name + " already exists (Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
        } else {
            if (cachedAdaptiveClass != null) {
                throw new IllegalStateException(
                    "Adaptive Extension already exists (Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
        }
    }

    /**
     * Replace the existing extension via API
     *
     * @param name  extension name
     * @param clazz extension class
     * @throws IllegalStateException when extension to be placed doesn't exist
     * @deprecated not recommended any longer, and use only when test
     */
    @Deprecated
    public void replaceExtension(String name, Class<?> clazz) {
        checkDestroyed();
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException(
                "Input type " + clazz + " doesn't implement Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " + clazz + " can't be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException(
                    "Extension name is blank (Extension " + type + ")!");
            }
            if (!cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException(
                    "Extension name " + name + " doesn't exist (Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
            cachedInstances.remove(name);
        } else {
            if (cachedAdaptiveClass == null) {
                throw new IllegalStateException(
                    "Adaptive Extension doesn't exist (Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
            cachedAdaptiveInstance.set(null);
        }
    }

    @SuppressWarnings("unchecked")
    public T getAdaptiveExtension() {
        checkDestroyed();
        Object instance = cachedAdaptiveInstance.get();
        if (instance == null) {
            if (createAdaptiveInstanceError != null) {
                throw new IllegalStateException(
                    "Failed to create adaptive instance: " + createAdaptiveInstanceError.toString(),
                    createAdaptiveInstanceError);
            }

            synchronized (cachedAdaptiveInstance) {
                instance = cachedAdaptiveInstance.get();
                if (instance == null) {
                    try {
                        instance = createAdaptiveExtension();
                        cachedAdaptiveInstance.set(instance);
                    } catch (Throwable t) {
                        createAdaptiveInstanceError = t;
                        throw new IllegalStateException(
                            "Failed to create adaptive instance: " + t.toString(), t);
                    }
                }
            }
        }

        return (T) instance;
    }

    private IllegalStateException findException(String name) {
        StringBuilder buf = new StringBuilder(
            "No such extension " + type.getName() + " by name " + name);

        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(name.toLowerCase())) {
                if (i == 1) {
                    buf.append(", possible causes: ");
                }
                buf.append("\r\n(");
                buf.append(i++);
                buf.append(") ");
                buf.append(entry.getKey());
                buf.append(":\r\n");
                buf.append(StringUtils.toString(entry.getValue()));
            }
        }

        if (i == 1) {
            buf.append(
                ", no related exception was found, please check whether related SPI module is missing.");
        }
        return new IllegalStateException(buf.toString());
    }

    @SuppressWarnings("unchecked")
    private T createExtension(String name, boolean wrap) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null || unacceptableExceptions.contains(name)) {
            throw findException(name);
        }
        try {
            T instance = (T) extensionInstances.get(clazz);
            if (instance == null) {
                extensionInstances.putIfAbsent(clazz, createExtensionInstance(clazz));
                instance = (T) extensionInstances.get(clazz);
                instance = postProcessBeforeInitialization(instance, name);
                injectExtension(instance);
                instance = postProcessAfterInitialization(instance, name);
            }

            if (wrap) {
                List<Class<?>> wrapperClassesList = new ArrayList<>();
                if (cachedWrapperClasses != null) {
                    wrapperClassesList.addAll(cachedWrapperClasses);
                    wrapperClassesList.sort(WrapperComparator.COMPARATOR);
                    Collections.reverse(wrapperClassesList);
                }

                if (CollectionUtils.isNotEmpty(wrapperClassesList)) {
                    for (Class<?> wrapperClass : wrapperClassesList) {
                        Wrapper wrapper = wrapperClass.getAnnotation(Wrapper.class);
                        boolean match = (wrapper == null) || ((ArrayUtils.isEmpty(
                            wrapper.matches()) || ArrayUtils.contains(wrapper.matches(),
                            name)) && !ArrayUtils.contains(wrapper.mismatches(), name));
                        if (match) {
                            instance = injectExtension(
                                (T) wrapperClass.getConstructor(type).newInstance(instance));
                            instance = postProcessAfterInitialization(instance, name);
                        }
                    }
                }
            }

            // Warning: After an instance of Lifecycle is wrapped by cachedWrapperClasses, it may not still be Lifecycle instance, this application may not invoke the lifecycle.initialize hook.
            initExtension(instance);
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException(
                "Extension instance (name: " + name + ", class: " + type + ") couldn't be instantiated: " + t.getMessage(),
                t);
        }
    }

    private Object createExtensionInstance(Class<?> type) throws ReflectiveOperationException {
        return instantiationStrategy.instantiate(type);
    }

    @SuppressWarnings("unchecked")
    private T postProcessBeforeInitialization(T instance, String name) throws Exception {
        if (extensionPostProcessors != null) {
            for (ExtensionPostProcessor processor : extensionPostProcessors) {
                instance = (T) processor.postProcessBeforeInitialization(instance, name);
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private T postProcessAfterInitialization(T instance, String name) throws Exception {
        if (instance instanceof ExtensionAccessorAware) {
            ((ExtensionAccessorAware) instance).setExtensionAccessor(extensionDirector);
        }
        if (extensionPostProcessors != null) {
            for (ExtensionPostProcessor processor : extensionPostProcessors) {
                instance = (T) processor.postProcessAfterInitialization(instance, name);
            }
        }
        return instance;
    }

    private boolean containsExtension(String name) {
        return getExtensionClasses().containsKey(name);
    }

    private T injectExtension(T instance) {
        if (injector == null) {
            return instance;
        }

        try {
            for (Method method : instance.getClass().getMethods()) {
                if (!isSetter(method)) {
                    continue;
                }
                /**
                 * Check {@link DisableInject} to see if we need auto-injection for this property
                 */
                if (method.isAnnotationPresent(DisableInject.class)) {
                    continue;
                }

                // When spiXXX implements ScopeModelAware, ExtensionAccessorAware,
                // the setXXX of ScopeModelAware and ExtensionAccessorAware does not need to be injected
                if (method.getDeclaringClass() == ScopeModelAware.class) {
                    continue;
                }
                if (instance instanceof ScopeModelAware || instance instanceof ExtensionAccessorAware) {
                    if (ignoredInjectMethodsDesc.contains(ReflectUtils.getDesc(method))) {
                        continue;
                    }
                }

                Class<?> pt = method.getParameterTypes()[0];
                if (ReflectUtils.isPrimitives(pt)) {
                    continue;
                }

                try {
                    String property = getSetterProperty(method);
                    Object object = injector.getInstance(pt, property);
                    if (object != null) {
                        method.invoke(instance, object);
                    }
                } catch (Exception e) {
                    logger.error(COMMON_ERROR_LOAD_EXTENSION, "", "",
                        "Failed to inject via method " + method.getName() + " of interface " + type.getName() + ": " + e.getMessage(),
                        e);
                }
            }
        } catch (Exception e) {
            logger.error(COMMON_ERROR_LOAD_EXTENSION, "", "", e.getMessage(), e);
        }
        return instance;
    }

    private void initExtension(T instance) {
        if (instance instanceof Lifecycle) {
            Lifecycle lifecycle = (Lifecycle) instance;
            lifecycle.initialize();
        }
    }

    /**
     * get properties name for setter, for instance: setVersion, return "version"
     * <p>
     * return "", if setter name with length less than 3
     */
    private String getSetterProperty(Method method) {
        return method.getName().length() > 3 ?
            method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) :
            "";
    }

    /**
     * return true if and only if:
     * <p>
     * 1, public
     * <p>
     * 2, name starts with "set"
     * <p>
     * 3, only has one parameter
     */
    private boolean isSetter(Method method) {
        return method.getName()
            .startsWith("set") && method.getParameterTypes().length == 1 && Modifier.isPublic(
            method.getModifiers());
    }

    private Class<?> getExtensionClass(String name) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Extension name == null");
        }
        return getExtensionClasses().get(name);
    }

    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    try {
                        classes = loadExtensionClasses();
                    } catch (InterruptedException e) {
                        logger.error(COMMON_ERROR_LOAD_EXTENSION, "", "",
                            "Exception occurred when loading extension class (interface: " + type + ")",
                            e);
                        throw new IllegalStateException(
                            "Exception occurred when loading extension class (interface: " + type + ")",
                            e);
                    }
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * synchronized in getExtensionClasses
     */
    @SuppressWarnings("deprecation")
    private Map<String, Class<?>> loadExtensionClasses() throws InterruptedException {
        checkDestroyed();
        cacheDefaultExtensionName();

        Map<String, Class<?>> extensionClasses = new HashMap<>();

        for (LoadingStrategy strategy : strategies) {
            loadDirectory(extensionClasses, strategy, type.getName());

            // compatible with old ExtensionFactory
            if (this.type == ExtensionInjector.class) {
                loadDirectory(extensionClasses, strategy, ExtensionFactory.class.getName());
            }
        }

        return extensionClasses;
    }

    private void loadDirectory(Map<String, Class<?>> extensionClasses, LoadingStrategy strategy,
                               String type) throws InterruptedException {
        loadDirectoryInternal(extensionClasses, strategy, type);
        try {
            String oldType = type.replace("org.apache", "com.alibaba");
            if (oldType.equals(type)) {
                return;
            }
            //if class not found,skip try to load resources
            ClassUtils.forName(oldType);
            loadDirectoryInternal(extensionClasses, strategy, oldType);
        } catch (ClassNotFoundException classNotFoundException) {

        }
    }

    /**
     * extract and cache default extension name if exists
     */
    private void cacheDefaultExtensionName() {
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation == null) {
            return;
        }

        String value = defaultAnnotation.value();
        if ((value = value.trim()).length() > 0) {
            String[] names = NAME_SEPARATOR.split(value);
            if (names.length > 1) {
                throw new IllegalStateException(
                    "More than 1 default extension name on extension " + type.getName() + ": " + Arrays.toString(
                        names));
            }
            if (names.length == 1) {
                cachedDefaultName = names[0];
            }
        }
    }

    private void loadDirectoryInternal(Map<String, Class<?>> extensionClasses,
                                       LoadingStrategy loadingStrategy, String type)
        throws InterruptedException {
        String fileName = loadingStrategy.directory() + type;
        try {
            List<ClassLoader> classLoadersToLoad = new LinkedList<>();

            // try to load from ExtensionLoader's ClassLoader first
            if (loadingStrategy.preferExtensionClassLoader()) {
                ClassLoader extensionLoaderClassLoader = ExtensionLoader.class.getClassLoader();
                if (ClassLoader.getSystemClassLoader() != extensionLoaderClassLoader) {
                    classLoadersToLoad.add(extensionLoaderClassLoader);
                }
            }

            if (specialSPILoadingStrategyMap.containsKey(type)) {
                String internalDirectoryType = specialSPILoadingStrategyMap.get(type);
                //skip to load spi when name don't match
                if (!LoadingStrategy.ALL.equals(
                    internalDirectoryType) && !internalDirectoryType.equals(
                    loadingStrategy.getName())) {
                    return;
                }
                classLoadersToLoad.clear();
                classLoadersToLoad.add(ExtensionLoader.class.getClassLoader());
            } else {
                // load from scope model
                Set<ClassLoader> classLoaders = scopeModel.getClassLoaders();

                if (CollectionUtils.isEmpty(classLoaders)) {
                    Enumeration<java.net.URL> resources = ClassLoader.getSystemResources(fileName);
                    if (resources != null) {
                        while (resources.hasMoreElements()) {
                            loadResource(extensionClasses, null, resources.nextElement(),
                                loadingStrategy.overridden(), loadingStrategy.includedPackages(),
                                loadingStrategy.excludedPackages(),
                                loadingStrategy.onlyExtensionClassLoaderPackages());
                        }
                    }
                } else {
                    classLoadersToLoad.addAll(classLoaders);
                }
            }

            Map<ClassLoader, Set<java.net.URL>> resources = ClassLoaderResourceLoader.loadResources(
                fileName, classLoadersToLoad);
            resources.forEach(((classLoader, urls) -> {
                loadFromClass(extensionClasses, loadingStrategy.overridden(), urls, classLoader,
                    loadingStrategy.includedPackages(), loadingStrategy.excludedPackages(),
                    loadingStrategy.onlyExtensionClassLoaderPackages());
            }));
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable t) {
            logger.error(COMMON_ERROR_LOAD_EXTENSION, "", "",
                "Exception occurred when loading extension class (interface: " + type + ", description file: " + fileName + ").",
                t);
        }
    }

    private void loadFromClass(Map<String, Class<?>> extensionClasses, boolean overridden,
                               Set<java.net.URL> urls, ClassLoader classLoader,
                               String[] includedPackages, String[] excludedPackages,
                               String[] onlyExtensionClassLoaderPackages) {
        if (CollectionUtils.isNotEmpty(urls)) {
            for (java.net.URL url : urls) {
                loadResource(extensionClasses, classLoader, url, overridden, includedPackages,
                    excludedPackages, onlyExtensionClassLoaderPackages);
            }
        }
    }

    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader,
                              java.net.URL resourceURL, boolean overridden,
                              String[] includedPackages, String[] excludedPackages,
                              String[] onlyExtensionClassLoaderPackages) {
        try {
            List<String> newContentList = getResourceContent(resourceURL);
            String clazz;
            for (String line : newContentList) {
                try {
                    String name = null;
                    int i = line.indexOf('=');
                    if (i > 0) {
                        name = line.substring(0, i).trim();
                        clazz = line.substring(i + 1).trim();
                    } else {
                        clazz = line;
                    }
                    if (StringUtils.isNotEmpty(clazz) && !isExcluded(clazz,
                        excludedPackages) && isIncluded(clazz,
                        includedPackages) && !isExcludedByClassLoader(clazz, classLoader,
                        onlyExtensionClassLoaderPackages)) {

                        loadClass(classLoader, extensionClasses, resourceURL,
                            Class.forName(clazz, true, classLoader), name, overridden);
                    }
                } catch (Throwable t) {
                    IllegalStateException e = new IllegalStateException(
                        "Failed to load extension class (interface: " + type + ", class line: " + line + ") in " + resourceURL + ", cause: " + t.getMessage(),
                        t);
                    exceptions.put(line, e);
                }
            }
        } catch (Throwable t) {
            logger.error(COMMON_ERROR_LOAD_EXTENSION, "", "",
                "Exception occurred when loading extension class (interface: " + type + ", class file: " + resourceURL + ") in " + resourceURL,
                t);
        }
    }

    private List<String> getResourceContent(java.net.URL resourceURL) throws IOException {
        Map<java.net.URL, List<String>> urlListMap = urlListMapCache.get();
        if (urlListMap == null) {
            synchronized (ExtensionLoader.class) {
                if ((urlListMap = urlListMapCache.get()) == null) {
                    urlListMap = new ConcurrentHashMap<>();
                    urlListMapCache = new SoftReference<>(urlListMap);
                }
            }
        }

        List<String> contentList = urlListMap.computeIfAbsent(resourceURL, key -> {
            List<String> newContentList = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceURL.openStream(), StandardCharsets.UTF_8))) {
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
        });
        return contentList;
    }

    private boolean isIncluded(String className, String... includedPackages) {
        if (includedPackages != null && includedPackages.length > 0) {
            for (String includedPackage : includedPackages) {
                if (className.startsWith(includedPackage + ".")) {
                    // one match, return true
                    return true;
                }
            }
            // none matcher match, return false
            return false;
        }
        // matcher is empty, return true
        return true;
    }

    private boolean isExcluded(String className, String... excludedPackages) {
        if (excludedPackages != null) {
            for (String excludePackage : excludedPackages) {
                if (className.startsWith(excludePackage + ".")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isExcludedByClassLoader(String className, ClassLoader classLoader,
                                            String... onlyExtensionClassLoaderPackages) {
        if (onlyExtensionClassLoaderPackages != null) {
            for (String excludePackage : onlyExtensionClassLoaderPackages) {
                if (className.startsWith(excludePackage + ".")) {
                    // if target classLoader is not ExtensionLoader's classLoader should be excluded
                    return !Objects.equals(ExtensionLoader.class.getClassLoader(), classLoader);
                }
            }
        }
        return false;
    }

    private void loadClass(ClassLoader classLoader, Map<String, Class<?>> extensionClasses,
                           java.net.URL resourceURL, Class<?> clazz, String name,
                           boolean overridden) {
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException(
                "Error occurred when loading extension class (interface: " + type + ", class line: " + clazz.getName() + "), class " + clazz.getName() + " is not subtype of interface.");
        }

        boolean isActive = loadClassIfActive(classLoader, clazz);

        if (!isActive) {
            return;
        }

        if (clazz.isAnnotationPresent(Adaptive.class)) {
            cacheAdaptiveClass(clazz, overridden);
        } else if (isWrapperClass(clazz)) {
            cacheWrapperClass(clazz);
        } else {
            if (StringUtils.isEmpty(name)) {
                name = findAnnotationName(clazz);
                if (name.length() == 0) {
                    throw new IllegalStateException(
                        "No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
                }
            }

            String[] names = NAME_SEPARATOR.split(name);
            if (ArrayUtils.isNotEmpty(names)) {
                cacheActivateClass(clazz, names[0]);
                for (String n : names) {
                    cacheName(clazz, n);
                    saveInExtensionClass(extensionClasses, clazz, n, overridden);
                }
            }
        }
    }

    private boolean loadClassIfActive(ClassLoader classLoader, Class<?> clazz) {
        Activate activate = clazz.getAnnotation(Activate.class);

        if (activate == null) {
            return true;
        }
        String[] onClass = null;

        if (activate instanceof Activate) {
            onClass = ((Activate) activate).onClass();
        } else if (activate instanceof com.alibaba.dubbo.common.extension.Activate) {
            onClass = ((com.alibaba.dubbo.common.extension.Activate) activate).onClass();
        }

        boolean isActive = true;

        if (null != onClass && onClass.length > 0) {
            isActive = Arrays.stream(onClass).filter(StringUtils::isNotBlank)
                .allMatch(className -> ClassUtils.isPresent(className, classLoader));
        }
        return isActive;
    }

    /**
     * cache name
     */
    private void cacheName(Class<?> clazz, String name) {
        if (!cachedNames.containsKey(clazz)) {
            cachedNames.put(clazz, name);
        }
    }

    /**
     * put clazz in extensionClasses
     */
    private void saveInExtensionClass(Map<String, Class<?>> extensionClasses, Class<?> clazz,
                                      String name, boolean overridden) {
        Class<?> c = extensionClasses.get(name);
        if (c == null || overridden) {
            extensionClasses.put(name, clazz);
        } else if (c != clazz) {
            // duplicate implementation is unacceptable
            unacceptableExceptions.add(name);
            String duplicateMsg = "Duplicate extension " + type.getName() + " name " + name + " on " + c.getName() + " and " + clazz.getName();
            logger.error(COMMON_ERROR_LOAD_EXTENSION, "", "", duplicateMsg);
            throw new IllegalStateException(duplicateMsg);
        }
    }

    /**
     * cache Activate class which is annotated with <code>Activate</code>
     * <p>
     * for compatibility, also cache class with old alibaba Activate annotation
     */
    @SuppressWarnings("deprecation")
    private void cacheActivateClass(Class<?> clazz, String name) {
        Activate activate = clazz.getAnnotation(Activate.class);
        if (activate != null) {
            cachedActivates.put(name, activate);
        } else {
            // support com.alibaba.dubbo.common.extension.Activate
            com.alibaba.dubbo.common.extension.Activate oldActivate = clazz.getAnnotation(
                com.alibaba.dubbo.common.extension.Activate.class);
            if (oldActivate != null) {
                cachedActivates.put(name, oldActivate);
            }
        }
    }

    /**
     * cache Adaptive class which is annotated with <code>Adaptive</code>
     */
    private void cacheAdaptiveClass(Class<?> clazz, boolean overridden) {
        if (cachedAdaptiveClass == null || overridden) {
            cachedAdaptiveClass = clazz;
        } else if (!cachedAdaptiveClass.equals(clazz)) {
            throw new IllegalStateException(
                "More than 1 adaptive class found: " + cachedAdaptiveClass.getName() + ", " + clazz.getName());
        }
    }

    /**
     * cache wrapper class
     * <p>
     * like: ProtocolFilterWrapper, ProtocolListenerWrapper
     */
    private void cacheWrapperClass(Class<?> clazz) {
        if (cachedWrapperClasses == null) {
            cachedWrapperClasses = new ConcurrentHashSet<>();
        }
        cachedWrapperClasses.add(clazz);
    }

    /**
     * test if clazz is a wrapper class
     * <p>
     * which has Constructor with given class type as its only argument
     */
    protected boolean isWrapperClass(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterTypes().length == 1 && constructor.getParameterTypes()[0] == type) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private String findAnnotationName(Class<?> clazz) {
        Extension extension = clazz.getAnnotation(Extension.class);
        if (extension != null) {
            return extension.value();
        }

        String name = clazz.getSimpleName();
        if (name.endsWith(type.getSimpleName())) {
            name = name.substring(0, name.length() - type.getSimpleName().length());
        }
        return name.toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private T createAdaptiveExtension() {
        try {
            T instance = (T) getAdaptiveExtensionClass().newInstance();
            instance = postProcessBeforeInitialization(instance, null);
            injectExtension(instance);
            instance = postProcessAfterInitialization(instance, null);
            initExtension(instance);
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Can't create adaptive extension " + type + ", cause: " + e.getMessage(), e);
        }
    }

    private Class<?> getAdaptiveExtensionClass() {
        getExtensionClasses();
        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }

    private Class<?> createAdaptiveExtensionClass() {
        // Adaptive Classes' ClassLoader should be the same with Real SPI interface classes' ClassLoader
        ClassLoader classLoader = type.getClassLoader();
        try {
            if (NativeUtils.isNative()) {
                return classLoader.loadClass(type.getName() + "$Adaptive");
            }
        } catch (Throwable ignore) {

        }
        String code = new AdaptiveClassCodeGenerator(type, cachedDefaultName).generate();
        org.apache.dubbo.common.compiler.Compiler compiler = extensionDirector.getExtensionLoader(
            org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
        return compiler.compile(type, code, classLoader);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }

    private static Properties loadProperties(ClassLoader classLoader, String resourceName) {
        Properties properties = new Properties();
        if (classLoader != null) {
            try {
                Enumeration<java.net.URL> resources = classLoader.getResources(resourceName);
                while (resources.hasMoreElements()) {
                    java.net.URL url = resources.nextElement();
                    Properties props = loadFromUrl(url);
                    for (Map.Entry<Object, Object> entry : props.entrySet()) {
                        String key = entry.getKey().toString();
                        if (properties.containsKey(key)) {
                            continue;
                        }
                        properties.put(key, entry.getValue().toString());
                    }
                }
            } catch (IOException ex) {
                logger.error(CONFIG_FAILED_LOAD_ENV_VARIABLE, "", "", "load properties failed.",
                    ex);
            }
        }

        return properties;
    }

    private static Properties loadFromUrl(java.net.URL url) {
        Properties properties = new Properties();
        InputStream is = null;
        try {
            is = url.openStream();
            properties.load(is);
        } catch (IOException e) {
            // ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return properties;
    }

}
