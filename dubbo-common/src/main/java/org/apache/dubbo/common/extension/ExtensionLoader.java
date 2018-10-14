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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.support.ActivateComparator;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassHelper;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * 拓展加载器。这是 Dubbo SPI 的核心
 * <p></p>
 * Load dubbo extensions
 * <ul>
 * <li>auto inject dependency extension </li>
 * <li>auto wrap extension in wrapper </li>
 * <li>default extension is an adaptive instance</li>
 * </ul>
 *
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#Service%20Provider">Service
 * Provider in Java 5</a>
 * @see org.apache.dubbo.common.extension.SPI
 * @see org.apache.dubbo.common.extension.Adaptive
 * @see org.apache.dubbo.common.extension.Activate
 */
public class ExtensionLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final String SERVICES_DIRECTORY = "META-INF/services/";

    private static final String DUBBO_DIRECTORY = "META-INF/dubbo/";

    private static final String DUBBO_INTERNAL_DIRECTORY = DUBBO_DIRECTORY + "internal/";

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    //////////////////////////////// 静态属性 //////////////////////////////////////////

    /**
     * 拓展加载器集合
     * key:拓展接口
     */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>, ExtensionLoader<?>>();

    /**
     * 拓展实现类集合
     * <p>
     * key：拓展实现类
     * value：拓展对象。
     * <p>
     * 例如，key 为 Class<AccessLogFilter>
     * value 为 AccessLogFilter 对象
     */
    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

    // ==============================对象属性===============================
    /**
     * 拓展接口。
     * 例如，Protocol
     */
    private final Class<?> type;
    /**
     * 对象工厂
     * <p>
     * 用于调用 {@link #injectExtension(Object)} 方法，向拓展对象注入依赖属性。
     * <p>
     * 例如，StubProxyFactoryWrapper 中有 `Protocol protocol` 属性。
     */
    private final ExtensionFactory objectFactory;
    /**
     * 缓存的拓展名与拓展类的映射。
     * <p>
     * 和 {@link #cachedClasses} 的 KV 对调。
     * <p>
     * 通过 {@link #loadExtensionClasses} 加载
     */
    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<Class<?>, String>();
    /**
     * 缓存的拓展实现类集合。
     * <p>
     * 不包含如下两种类型：
     * 1. 自适应拓展实现类。例如 AdaptiveExtensionFactory
     * 2. 带唯一参数为拓展接口的构造方法的实现类，或者说拓展 Wrapper 实现类。例如，ProtocolFilterWrapper 。
     * 拓展 Wrapper 实现类，会添加到 {@link #cachedWrapperClasses} 中
     * <p>
     * 通过 {@link #loadExtensionClasses} 加载
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<Map<String, Class<?>>>();
    /**
     * 拓展名与 @Activate 的映射
     * <p>
     * 例如，AccessLogFilter。
     * <p>
     * 用于 {@link #getActivateExtension(URL, String)}
     */
    private final Map<String, Object> cachedActivates = new ConcurrentHashMap<String, Object>();
    /**
     * 缓存的拓展对象集合
     * <p>
     * key：拓展名
     * value：拓展对象
     * <p>
     * 例如，Protocol 拓展
     * key：dubbo value：DubboProtocol
     * key：injvm value：InjvmProtocol
     * <p>
     * 通过 {@link #loadExtensionClasses} 加载
     */
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();
    /**
     * 缓存的自适应( Adaptive )拓展对象
     */
    private final Holder<Object> cachedAdaptiveInstance = new Holder<Object>();
    /**
     * 缓存的自适应拓展对象的类
     * <p>
     * {@link #getAdaptiveExtensionClass()}
     */
    private volatile Class<?> cachedAdaptiveClass = null;
    /**
     * 缓存的默认拓展名
     * <p>
     * 通过 {@link SPI} 注解获得
     */
    private String cachedDefaultName;
    /**
     * 创建 {@link #cachedAdaptiveInstance} 时发生的异常。
     * <p>
     * 发生异常后，不再创建，参见 {@link #createAdaptiveExtension()}
     */
    private volatile Throwable createAdaptiveInstanceError;
    /**
     * 拓展 Wrapper 实现类集合
     * <p>
     * 带唯一参数为拓展接口的构造方法的实现类
     * <p>
     * 通过 {@link #loadExtensionClasses} 加载
     */
    private Set<Class<?>> cachedWrapperClasses;
    /**
     * 拓展名 与 加载对应拓展类发生的异常 的 映射
     * <p>
     * key：拓展名
     * value：异常
     * <p>
     * 在 {@link #loadDirectory(Map, String, String)} 时，记录
     */
    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<String, IllegalStateException>();

    private ExtensionLoader(Class<?> type) {
        this.type = type;
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }

    /**
     * 是否包含 @SPI 注解
     *
     * @param type 类
     * @param <T>  泛型
     * @return 是否包含
     */
    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    /**
     * 根据拓展点的接口，获得拓展加载器
     *
     * @param type 接口
     * @param <T>  泛型
     * @return 加载器
     */
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        // 必须是接口
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
        }
        // 必须包含 @SPI 注解
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type(" + type +
                    ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }
        // 获得接口对应的拓展点加载器
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }

    private static ClassLoader findClassLoader() {
        return ClassHelper.getClassLoader(ExtensionLoader.class);
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
     * This is equivalent to {@code getActivateExtension(url, url.getParameter(key).split(","),
     * null)}
     * 获得符合自动激活条件的拓展对象数组
     *
     * @param url   url
     * @param key   url parameter key which used to get extension point names
     * @param group group
     * @return extension list which are activated.
     * @see #getActivateExtension(org.apache.dubbo.common.URL, String[], String)
     */
    public List<T> getActivateExtension(URL url, String key, String group) {
        // 从 Dubbo URL 获得参数值
        String value = url.getParameter(key);
        // 获得符合自动激活条件的拓展对象数组
        return getActivateExtension(url, value == null || value.length() == 0 ? null : Constants.COMMA_SPLIT_PATTERN.split(value), group);
    }

    /**
     * Get activate extensions.
     * 获得符合自动激活条件的拓展对象数组
     *
     * @param url    url
     * @param values extension point names
     * @param group  group
     * @return extension list which are activated
     * @see org.apache.dubbo.common.extension.Activate
     */
    public List<T> getActivateExtension(URL url, String[] values, String group) {
        List<T> exts = new ArrayList<T>();
        List<String> names = values == null ? new ArrayList<String>(0) : Arrays.asList(values);
        // 处理自动激活的拓展对象们
        // 判断不存在配置 `"-name"` 。例如，<dubbo:service filter="-default" /> ，代表移除所有默认过滤器。
        if (!names.contains(Constants.REMOVE_VALUE_PREFIX + Constants.DEFAULT_KEY)) {
            // 获得拓展实现类数组
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
                // 匹配分组
                if (isMatchGroup(group, activateGroup)) {
                    // 获得拓展对象
                    T ext = getExtension(name);
                    // 不包含在自定义配置里。如果包含，会在下面的代码处理。
                    // 判断是否配置移除。例如 <dubbo:service filter="-monitor" />，则 MonitorFilter 会被移除
                    // 判断是否激活
                    if (!names.contains(name)
                            && !names.contains(Constants.REMOVE_VALUE_PREFIX + name)
                            && isActive(activateValue, url)) {
                        exts.add(ext);
                    }
                }
            }
            // 排序
            Collections.sort(exts, ActivateComparator.COMPARATOR);
        }
        // 处理自定义配置的拓展对象们。例如在 <dubbo:service filter="demo" /> ，代表需要加入 DemoFilter （这个是笔者自定义的）。
        List<T> usrs = new ArrayList<T>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            // 判断非移除的
            if (!name.startsWith(Constants.REMOVE_VALUE_PREFIX)
                    && !names.contains(Constants.REMOVE_VALUE_PREFIX + name)) {
                // 将配置的自定义在自动激活的拓展对象们前面。例如，<dubbo:service filter="demo,default,demo2" /> ，则 DemoFilter 就会放在默认的过滤器前面。
                if (Constants.DEFAULT_KEY.equals(name)) {
                    if (!usrs.isEmpty()) {
                        exts.addAll(0, usrs);
                        usrs.clear();
                    }
                } else {
                    // 获得拓展对象
                    T ext = getExtension(name);
                    usrs.add(ext);
                }
            }
        }
        // 添加到结果集
        if (!usrs.isEmpty()) {
            exts.addAll(usrs);
        }
        return exts;
    }

    /**
     * 匹配分组
     *
     * @param group  过滤的分组条件。若为空，无需过滤
     * @param groups 配置的分组
     * @return 是否匹配
     */
    private boolean isMatchGroup(String group, String[] groups) {
        // 为空，无需过滤
        if (group == null || group.length() == 0) {
            return true;
        }
        //过滤
        if (groups != null && groups.length > 0) {
            for (String g : groups) {
                if (group.equals(g)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isActive(String[] keys, URL url) {
        if (keys.length == 0) {
            return true;
        }
        for (String key : keys) {
            for (Map.Entry<String, String> entry : url.getParameters().entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if ((k.equals(key) || k.endsWith("." + key))
                        && ConfigUtils.isNotEmpty(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get extension's instance. Return <code>null</code> if extension is not found or is not
     * initialized. Pls. note that this method will not trigger extension load.
     * <p>
     * In order to trigger extension load, call {@link #getExtension(String)} instead.
     *
     * @see #getExtension(String)
     */
    /**
     * 返回扩展点实例，如果没有指定的扩展点或是还没加载（即实例化）则返回<code>null</code>。注意：此方法不会触发扩展点的加载。
     * <p/>
     * 一般应该调用{@link #getExtension(String)}方法获得扩展，这个方法会触发扩展点加载。
     *
     * @see #getExtension(String)
     */
    @SuppressWarnings("unchecked")
    public T getLoadedExtension(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Extension name == null");
        }
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        return (T) holder.get();
    }

    /**
     * Return the list of extensions which are already loaded.
     * <p>
     * Usually {@link #getSupportedExtensions()} should be called in order to get all extensions.
     *
     * @see #getSupportedExtensions()
     */
    /**
     * 返回已经加载的扩展点的名字。
     * <p/>
     * 一般应该调用{@link #getSupportedExtensions()}方法获得扩展，这个方法会返回所有的扩展点。
     *
     * @see #getSupportedExtensions()
     */
    public Set<String> getLoadedExtensions() {
        return Collections.unmodifiableSet(new TreeSet<String>(cachedInstances.keySet()));
    }

    /**
     * Find the extension with the given name. If the specified name is not found, then {@link
     * IllegalStateException} will be thrown.
     */
    /**
     * 返回指定名字的扩展对象。如果指定名字的扩展不存在，则抛异常 {@link IllegalStateException}.
     *
     * @param name 拓展名
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    public T getExtension(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Extension name == null");
        }
        // 查找 默认的 拓展对象
        if ("true".equals(name)) {
            return getDefaultExtension();
        }
        Holder<Object> holder = cachedInstances.get(name);
        // 从 缓存中 获得对应的拓展对象
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                // 从 缓存中 未获取到，进行创建缓存对象。
                if (instance == null) {
                    instance = createExtension(name);
                    // 设置创建对象到缓存中
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * Return default extension, return <code>null</code> if it's not configured.
     */
    /**
     * 返回缺省的扩展，如果没有设置则返回<code>null</code>。
     */
    public T getDefaultExtension() {
        getExtensionClasses();
        // 如果为 true ，不能继续调用 `#getExtension(true)` 方法，会形成死循环。
        if (null == cachedDefaultName || cachedDefaultName.length() == 0
                || "true".equals(cachedDefaultName)) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    public boolean hasExtension(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Extension name == null");
        }
        try {
            this.getExtensionClass(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public Set<String> getSupportedExtensions() {
        Map<String, Class<?>> clazzes = getExtensionClasses();
        return Collections.unmodifiableSet(new TreeSet<String>(clazzes.keySet()));
    }

    /**
     * Return default extension name, return <code>null</code> if not configured.
     * 返回缺省的扩展点名，如果没有设置缺省则返回<code>null</code>。
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
    /**
     * 编程方式添加新扩展点。
     *
     * @param name  扩展点名
     * @param clazz 扩展点类
     * @throws IllegalStateException 要添加扩展点名已经存在。
     */
    public void addExtension(String name, Class<?> clazz) {
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                    clazz + "not implement Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                    clazz + "can not be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " +
                        name + " already existed(Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
        } else {
            if (cachedAdaptiveClass != null) {
                throw new IllegalStateException("Adaptive Extension already existed(Extension " + type + ")!");
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
    /**
     * 编程方式添加替换已有扩展点。
     *
     * @param name  扩展点名
     * @param clazz 扩展点类
     * @throws IllegalStateException 要添加扩展点名已经存在。
     * @deprecated 不推荐应用使用，一般只在测试时可以使用
     */
    @Deprecated
    public void replaceExtension(String name, Class<?> clazz) {
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                    clazz + "not implement Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                    clazz + "can not be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (!cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " +
                        name + " not existed(Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
            cachedInstances.remove(name);
        } else {
            if (cachedAdaptiveClass == null) {
                throw new IllegalStateException("Adaptive Extension not existed(Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
            cachedAdaptiveInstance.set(null);
        }
    }

    /**
     * 获得自适应拓展对象
     *
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    public T getAdaptiveExtension() {
        // 从缓存中，获得自适应拓展对象
        Object instance = cachedAdaptiveInstance.get();
        if (instance == null) {
            // 若之前未创建报错，
            if (createAdaptiveInstanceError == null) {
                synchronized (cachedAdaptiveInstance) {
                    instance = cachedAdaptiveInstance.get();
                    if (instance == null) {
                        try {
                            // 创建自适应拓展对象
                            instance = createAdaptiveExtension();
                            // 设置到缓存
                            cachedAdaptiveInstance.set(instance);
                        } catch (Throwable t) {
                            // 记录异常
                            createAdaptiveInstanceError = t;
                            throw new IllegalStateException("fail to create adaptive instance: " + t.toString(), t);
                        }
                    }
                }
                // 若之前创建报错，则抛出异常 IllegalStateException
            } else {
                throw new IllegalStateException("fail to create adaptive instance: " + createAdaptiveInstanceError.toString(), createAdaptiveInstanceError);
            }
        }
        return (T) instance;
    }

    /**
     * 获得拓展名不存在时的异常
     *
     * @param name 拓展名
     * @return 异常
     */
    private IllegalStateException findException(String name) {
        // 在 `#loadFile(...)` 方法中，加载时，发生异常
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);


        int i = 1;
        // 生成不存在该拓展类实现的异常。
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
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
        return new IllegalStateException(buf.toString());
    }

    /**
     * 创建拓展名的拓展对象，并缓存。
     *
     * @param name 拓展名
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        // 获得拓展名对应的拓展实现类
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name); // 抛出异常
        }
        try {
            // 从缓存中，获得拓展对象。
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                // 当缓存不存在时，创建拓展对象，并添加到缓存中。
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            // 注入依赖的属性
            injectExtension(instance);
            // 创建 Wrapper 拓展对象
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (wrapperClasses != null && !wrapperClasses.isEmpty()) {
                for (Class<?> wrapperClass : wrapperClasses) {
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                    type + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }

    /**
     * 注入依赖的属性
     *
     * @param instance 拓展对象
     * @return 拓展对象
     */
    private T injectExtension(T instance) {
        try {
            if (objectFactory != null) {
                for (Method method : instance.getClass().getMethods()) {
                    if (method.getName().startsWith("set")
                            && method.getParameterTypes().length == 1
                            && Modifier.isPublic(method.getModifiers())) { // setting && public 方法
                        // 获得属性的类型
                        Class<?> pt = method.getParameterTypes()[0];
                        try {
                            // 获得属性
                            String property = method.getName().length() > 3 ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) : "";
                            // 获得属性值
                            Object object = objectFactory.getExtension(pt, property);
                            // 设置属性值
                            if (object != null) {
                                method.invoke(instance, object);
                            }
                        } catch (Exception e) {
                            logger.error("fail to inject via method " + method.getName()
                                    + " of interface " + type.getName() + ": " + e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return instance;
    }

    private Class<?> getExtensionClass(String name) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Extension name == null");
        }
        // 获得拓展实现类
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new IllegalStateException("No such extension \"" + name + "\" for " + type.getName() + "!");
        }
        return clazz;
    }

    /**
     * 获得拓展实现类数组
     *
     * @return 拓展实现类数组
     */
    private Map<String, Class<?>> getExtensionClasses() {
        // 从缓存中，获得拓展实现类数组
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    // 从配置文件中，加载拓展实现类数组
                    classes = loadExtensionClasses();
                    // 设置到缓存中
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    // synchronized in getExtensionClasses

    /**
     * 加载拓展实现类数组
     * <p>
     * 无需声明 synchronized ，因为唯一调用该方法的 {@link #getExtensionClasses()} 已经声明。
     * // synchronized in getExtensionClasses
     *
     * @return 拓展实现类数组
     */
    private Map<String, Class<?>> loadExtensionClasses() {
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation != null) {
            String value = defaultAnnotation.value();
            if ((value = value.trim()).length() > 0) {
                String[] names = NAME_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("more than 1 default extension name on extension " + type.getName()
                            + ": " + Arrays.toString(names));
                }
                if (names.length == 1) {
                    cachedDefaultName = names[0];
                }
            }
        }

        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
        loadDirectory(extensionClasses, DUBBO_INTERNAL_DIRECTORY, type.getName());
        loadDirectory(extensionClasses, DUBBO_INTERNAL_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
        loadDirectory(extensionClasses, DUBBO_DIRECTORY, type.getName());
        loadDirectory(extensionClasses, DUBBO_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
        loadDirectory(extensionClasses, SERVICES_DIRECTORY, type.getName());
        loadDirectory(extensionClasses, SERVICES_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
        return extensionClasses;
    }

    private void loadDirectory(Map<String, Class<?>> extensionClasses, String dir, String type) {
        String fileName = dir + type;
        try {
            Enumeration<java.net.URL> urls;
            ClassLoader classLoader = findClassLoader();
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL resourceURL = urls.nextElement();
                    loadResource(extensionClasses, classLoader, resourceURL);
                }
            }
        } catch (Throwable t) {
            logger.error("Exception when load extension class(interface: " +
                    type + ", description file: " + fileName + ").", t);
        }
    }

    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, java.net.URL resourceURL) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(resourceURL.openStream(), "utf-8"));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    final int ci = line.indexOf('#');
                    if (ci >= 0) {
                        line = line.substring(0, ci);
                    }
                    line = line.trim();
                    if (line.length() > 0) {
                        try {
                            String name = null;
                            int i = line.indexOf('=');
                            if (i > 0) {
                                name = line.substring(0, i).trim();
                                line = line.substring(i + 1).trim();
                            }
                            if (line.length() > 0) {
                                loadClass(extensionClasses, resourceURL, Class.forName(line, true, classLoader), name);
                            }
                        } catch (Throwable t) {
                            IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + resourceURL + ", cause: " + t.getMessage(), t);
                            exceptions.put(line, e);
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch (Throwable t) {
            logger.error("Exception when load extension class(interface: " +
                    type + ", class file: " + resourceURL + ") in " + resourceURL, t);
        }
    }

    private void loadClass(Map<String, Class<?>> extensionClasses, java.net.URL resourceURL, Class<?> clazz, String name) throws NoSuchMethodException {
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error when load extension class(interface: " +
                    type + ", class line: " + clazz.getName() + "), class "
                    + clazz.getName() + "is not subtype of interface.");
        }
        if (clazz.isAnnotationPresent(Adaptive.class)) {
            if (cachedAdaptiveClass == null) {
                cachedAdaptiveClass = clazz;
            } else if (!cachedAdaptiveClass.equals(clazz)) {
                throw new IllegalStateException("More than 1 adaptive class found: "
                        + cachedAdaptiveClass.getClass().getName()
                        + ", " + clazz.getClass().getName());
            }
        } else if (isWrapperClass(clazz)) {
            Set<Class<?>> wrappers = cachedWrapperClasses;
            if (wrappers == null) {
                cachedWrapperClasses = new ConcurrentHashSet<Class<?>>();
                wrappers = cachedWrapperClasses;
            }
            wrappers.add(clazz);
        } else {
            clazz.getConstructor();
            if (name == null || name.length() == 0) {
                name = findAnnotationName(clazz);
                if (name.length() == 0) {
                    throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
                }
            }
            String[] names = NAME_SEPARATOR.split(name);
            if (names != null && names.length > 0) {
                Activate activate = clazz.getAnnotation(Activate.class);
                if (activate != null) {
                    cachedActivates.put(names[0], activate);
                } else {
                    // support com.alibaba.dubbo.common.extension.Activate
                    com.alibaba.dubbo.common.extension.Activate oldActivate = clazz.getAnnotation(com.alibaba.dubbo.common.extension.Activate.class);
                    if (oldActivate != null) {
                        cachedActivates.put(names[0], oldActivate);
                    }
                }
                for (String n : names) {
                    if (!cachedNames.containsKey(clazz)) {
                        cachedNames.put(clazz, n);
                    }
                    Class<?> c = extensionClasses.get(n);
                    if (c == null) {
                        extensionClasses.put(n, clazz);
                    } else if (c != clazz) {
                        throw new IllegalStateException("Duplicate extension " + type.getName() + " name " + n + " on " + c.getName() + " and " + clazz.getName());
                    }
                }
            }
        }
    }

    private boolean isWrapperClass(Class<?> clazz) {
        try {
            clazz.getConstructor(type);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private String findAnnotationName(Class<?> clazz) {
        org.apache.dubbo.common.Extension extension = clazz.getAnnotation(org.apache.dubbo.common.Extension.class);
        if (extension == null) {
            String name = clazz.getSimpleName();
            if (name.endsWith(type.getSimpleName())) {
                name = name.substring(0, name.length() - type.getSimpleName().length());
            }
            return name.toLowerCase();
        }
        return extension.value();
    }

    /**
     * 创建自适应拓展对象
     *
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    private T createAdaptiveExtension() {
        try {
            return injectExtension((T) getAdaptiveExtensionClass().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException("Can not create adaptive extension " + type + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * @return 自适应拓展类
     */
    private Class<?> getAdaptiveExtensionClass() {
        getExtensionClasses();
        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }

    /**
     * 自动生成自适应拓展的代码实现，并编译后返回该类。
     *
     * @return 类
     */
    private Class<?> createAdaptiveExtensionClass() {
        // 自动生成自适应拓展的代码实现的字符串
        String code = createAdaptiveExtensionClassCode();
        // 编译代码，并返回该类
        ClassLoader classLoader = findClassLoader();
        org.apache.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
        return compiler.compile(code, classLoader);
    }

    /**
     * 自动生成自适应拓展的代码实现的字符串
     *
     * @return 代码字符串
     */
    private String createAdaptiveExtensionClassCode() {
        StringBuilder codeBuidler = new StringBuilder();
        // 遍历方法数组，判断有 @Adaptive 注解
        Method[] methods = type.getMethods();
        boolean hasAdaptiveAnnotation = false;
        for (Method m : methods) {
            if (m.isAnnotationPresent(Adaptive.class)) {
                hasAdaptiveAnnotation = true;
                break;
            }
        }
        // no need to generate adaptive class since there's no adaptive method found.
        // 完全没有Adaptive方法，则不需要生成Adaptive类
        if (!hasAdaptiveAnnotation)
            throw new IllegalStateException("No adaptive method on extension " + type.getName() + ", refuse to create the adaptive class!");

        // 生成代码：package 和 import
        codeBuidler.append("package " + type.getPackage().getName() + ";");
        codeBuidler.append("\nimport " + ExtensionLoader.class.getName() + ";");
        // 生成代码：类名
        codeBuidler.append("\npublic class " + type.getSimpleName() + "$Adaptive" + " implements " + type.getCanonicalName() + " {");

        // 循环方法
        for (Method method : methods) {
            Class<?> rt = method.getReturnType(); // 返回类型
            Class<?>[] pts = method.getParameterTypes(); // 参数类型数组
            Class<?>[] ets = method.getExceptionTypes(); // 异常类型数组

            Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class);
            StringBuilder code = new StringBuilder(512); // 方法体的代码
            // 非 @Adaptive 注解，生成代码：生成的方法为直接抛出异常。因为，非自适应的接口不应该被调用。
            if (adaptiveAnnotation == null) {
                code.append("throw new UnsupportedOperationException(\"method ")
                        .append(method.toString()).append(" of interface ")
                        .append(type.getName()).append(" is not adaptive method!\");");
                // @Adaptive 注解，生成方法体的代码
            } else {
                // 寻找 Dubbo URL 参数的位置
                int urlTypeIndex = -1;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].equals(URL.class)) {
                        urlTypeIndex = i;
                        break;
                    }
                }
                // found parameter in URL type
                // 有类型为URL的参数，生成代码：生成校验 URL 非空的代码
                if (urlTypeIndex != -1) {
                    // Null Point check
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"url == null\");",
                            urlTypeIndex);
                    code.append(s);

                    s = String.format("\n%s url = arg%d;", URL.class.getName(), urlTypeIndex);
                    code.append(s);
                }
                // did not find parameter in URL type
                // 参数没有URL类型
                else {
                    String attribMethod = null;

                    // find URL getter method
                    // 找到参数的URL属性 。例如，Invoker 有 `#getURL()` 方法。
                    LBL_PTS:
                    for (int i = 0; i < pts.length; ++i) {
                        Method[] ms = pts[i].getMethods();
                        for (Method m : ms) {
                            String name = m.getName();
                            if ((name.startsWith("get") || name.length() > 3)
                                    && Modifier.isPublic(m.getModifiers())
                                    && !Modifier.isStatic(m.getModifiers())
                                    && m.getParameterTypes().length == 0
                                    && m.getReturnType() == URL.class) { // pubic && getting 方法
                                urlTypeIndex = i;
                                attribMethod = name;
                                break LBL_PTS;
                            }
                        }
                    }
                    // 未找到，抛出异常。
                    if (attribMethod == null) {
                        throw new IllegalStateException("fail to create adaptive class for interface " + type.getName()
                                + ": not found url parameter or url attribute in parameters of method " + method.getName());
                    }

                    // 生成代码：校验 URL 非空
                    // Null point check
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"%s argument == null\");",
                            urlTypeIndex, pts[urlTypeIndex].getName());
                    code.append(s);
                    s = String.format("\nif (arg%d.%s() == null) throw new IllegalArgumentException(\"%s argument %s() == null\");",
                            urlTypeIndex, attribMethod, pts[urlTypeIndex].getName(), attribMethod);
                    code.append(s);

                    // 生成 `URL url = arg%d.%s();` 的代码
                    s = String.format("%s url = arg%d.%s();", URL.class.getName(), urlTypeIndex, attribMethod);
                    code.append(s);
                }

                String[] value = adaptiveAnnotation.value();
                // value is not set, use the value generated from class name as the key
                // 没有设置Key，则使用“扩展点接口名的点分隔 作为Key
                if (value.length == 0) {
                    char[] charArray = type.getSimpleName().toCharArray();
                    StringBuilder sb = new StringBuilder(128);
                    for (int i = 0; i < charArray.length; i++) {
                        if (Character.isUpperCase(charArray[i])) {
                            if (i != 0) {
                                sb.append(".");
                            }
                            sb.append(Character.toLowerCase(charArray[i]));
                        } else {
                            sb.append(charArray[i]);
                        }
                    }
                    value = new String[]{sb.toString()};
                }

                // 判断是否有 Invocation 参数
                boolean hasInvocation = false;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].getName().equals("com.alibaba.dubbo.rpc.Invocation")) {
                        // 生成代码：校验 Invocation 非空
                        // Null Point check
                        String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"invocation == null\");", i);
                        code.append(s);

                        // 生成代码：获得方法名
                        s = String.format("\nString methodName = arg%d.getMethodName();", i);
                        code.append(s);

                        // 标记有 Invocation 参数
                        hasInvocation = true;
                        break;
                    }
                }

                // 默认拓展名
                String defaultExtName = cachedDefaultName;
                // 获得最终拓展名的代码字符串，例如：
                // 【简单】1. url.getParameter("proxy", "javassist")
                // 【复杂】2. url.getParameter(key1, url.getParameter(key2, defaultExtName))
                String getNameCode = null;
                for (int i = value.length - 1; i >= 0; --i) { // 倒序的原因，因为是顺序获取参数，参见【复杂】2. 的例子
                    if (i == value.length - 1) {
                        if (null != defaultExtName) {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation) // 当【有】 Invocation 参数时，使用 `URL#getMethodParameter()` 方法。
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                                else // 当【非】 Invocation 参数时，使用 `URL#getParameter()` 方法。
                                    getNameCode = String.format("url.getParameter(\"%s\", \"%s\")", value[i], defaultExtName);
                            else // 当属性名是 "protocol" ，使用 `URL#getProtocl()` 方法获取。
                                getNameCode = String.format("( url.getProtocol() == null ? \"%s\" : url.getProtocol() )", defaultExtName);
                        } else {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation)
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName); // 此处的 defaultExtName ，可以去掉的。
                                else
                                    getNameCode = String.format("url.getParameter(\"%s\")", value[i]);
                            else
                                getNameCode = "url.getProtocol()";
                        }
                    } else {
                        if (!"protocol".equals(value[i]))
                            if (hasInvocation)
                                getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                            else
                                getNameCode = String.format("url.getParameter(\"%s\", %s)", value[i], getNameCode);
                        else
                            getNameCode = String.format("url.getProtocol() == null ? (%s) : url.getProtocol()", getNameCode);
                    }
                }

                // 生成代码：获取参数的代码。例如：String extName = url.getParameter("proxy", "javassist");
                code.append("\nString extName = ").append(getNameCode).append(";");
                // check extName == null?
                String s = String.format("\nif(extName == null) " +
                                "throw new IllegalStateException(\"Fail to get extension(%s) name from url(\" + url.toString() + \") use keys(%s)\");",
                        type.getName(), Arrays.toString(value));
                code.append(s);

                // 生成代码：拓展对象，调用方法。例如
                // `com.alibaba.dubbo.rpc.ProxyFactory extension = (com.alibaba.dubbo.rpc.ProxyFactory) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.ProxyFactory.class)
                //                                                                                                           .getExtension(extName);` 。
                s = String.format("\n%s extension = (%<s)%s.getExtensionLoader(%s.class).getExtension(extName);",
                        type.getName(), ExtensionLoader.class.getSimpleName(), type.getName());
                code.append(s);

                // return statement
                if (!rt.equals(void.class)) {
                    code.append("\nreturn ");
                }

                s = String.format("extension.%s(", method.getName());
                code.append(s);
                for (int i = 0; i < pts.length; i++) {
                    if (i != 0)
                        code.append(", ");
                    code.append("arg").append(i);
                }
                code.append(");");
            }

            // 生成方法
            codeBuidler.append("\npublic " + rt.getCanonicalName() + " " + method.getName() + "(");
            for (int i = 0; i < pts.length; i++) {
                if (i > 0) {
                    codeBuidler.append(", ");
                }
                codeBuidler.append(pts[i].getCanonicalName());
                codeBuidler.append(" ");
                codeBuidler.append("arg" + i);
            }
            codeBuidler.append(")");
            if (ets.length > 0) {
                codeBuidler.append(" throws ");  // 异常
                for (int i = 0; i < ets.length; i++) {
                    if (i > 0) {
                        codeBuidler.append(", ");
                    }
                    codeBuidler.append(ets[i].getCanonicalName());
                }
            }
            codeBuidler.append(" {");
            codeBuidler.append(code.toString());
            codeBuidler.append("\n}");
        }

        // 生成类末尾的 `}`
        codeBuidler.append("\n}");

        // 调试，打印生成的代码
        if (logger.isDebugEnabled()) {
            logger.debug(codeBuidler.toString());
        }
        return codeBuidler.toString();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }

}
