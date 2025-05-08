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
package org.apache.dubbo.common.beans.factory;

import org.apache.dubbo.common.beans.ScopeBeanException;
import org.apache.dubbo.common.beans.support.InstantiationStrategy;
import org.apache.dubbo.common.extension.ExtensionAccessor;
import org.apache.dubbo.common.extension.ExtensionAccessorAware;
import org.apache.dubbo.common.extension.ExtensionPostProcessor;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.resource.Disposable;
import org.apache.dubbo.common.resource.Initializable;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.TypeUtils;
import org.apache.dubbo.rpc.model.ScopeModelAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_DESTROY_INVOKER;

/**
 * A bean factory for internal sharing.
 */
public final class ScopeBeanFactory {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(ScopeBeanFactory.class);

    private final ScopeBeanFactory parent;
    private final ExtensionAccessor extensionAccessor;
    private final List<ExtensionPostProcessor> extensionPostProcessors;
    private final Map<Class<?>, AtomicInteger> beanNameIdCounterMap = CollectionUtils.newConcurrentHashMap();
    private final List<BeanInfo> registeredBeanInfos = new CopyOnWriteArrayList<>();
    private final List<BeanDefinition<?>> registeredBeanDefinitions = new CopyOnWriteArrayList<>();
    private InstantiationStrategy instantiationStrategy;
    private final AtomicBoolean destroyed = new AtomicBoolean();
    private final Set<Class<?>> registeredClasses = new ConcurrentHashSet<>();
    private final Map<Pair<Class<?>, String>, Optional<Object>> beanCache = CollectionUtils.newConcurrentHashMap();

    public ScopeBeanFactory(ScopeBeanFactory parent, ExtensionAccessor extensionAccessor) {
        this.parent = parent;
        this.extensionAccessor = extensionAccessor;
        extensionPostProcessors = extensionAccessor.getExtensionDirector().getExtensionPostProcessors();
        initInstantiationStrategy();
    }

    private void initInstantiationStrategy() {
        for (ExtensionPostProcessor extensionPostProcessor : extensionPostProcessors) {
            if (extensionPostProcessor instanceof ScopeModelAccessor) {
                instantiationStrategy = new InstantiationStrategy((ScopeModelAccessor) extensionPostProcessor);
                break;
            }
        }
        if (instantiationStrategy == null) {
            instantiationStrategy = new InstantiationStrategy();
        }
    }

    public <T> T registerBean(Class<T> clazz) throws ScopeBeanException {
        return getOrRegisterBean(null, clazz);
    }

    public <T> T registerBean(String name, Class<T> clazz) throws ScopeBeanException {
        return getOrRegisterBean(name, clazz);
    }

    public <T> void registerBeanDefinition(Class<T> clazz) {
        registerBeanDefinition(null, clazz);
    }

    public <T> void registerBeanDefinition(String name, Class<T> clazz) {
        registeredBeanDefinitions.add(new BeanDefinition<>(name, clazz));
    }

    public <T> void registerBeanFactory(Supplier<T> factory) {
        registerBeanFactory(null, factory);
    }

    @SuppressWarnings("unchecked")
    public <T> void registerBeanFactory(String name, Supplier<T> factory) {
        Class<T> clazz = (Class<T>) TypeUtils.getSuperGenericType(factory.getClass(), 0);
        if (clazz == null) {
            throw new ScopeBeanException("unable to determine bean class from factory's superclass or interface");
        }
        registeredBeanDefinitions.add(new BeanDefinition<>(name, clazz, factory));
    }

    private <T> T createAndRegisterBean(String name, Class<T> clazz) {
        checkDestroyed();
        T instance = getBean(name, clazz);
        if (instance != null) {
            throw new ScopeBeanException(
                    "already exists bean with same name and type, name=" + name + ", type=" + clazz.getName());
        }
        try {
            instance = instantiationStrategy.instantiate(clazz);
        } catch (Throwable e) {
            throw new ScopeBeanException("create bean instance failed, type=" + clazz.getName(), e);
        }
        registerBean(name, instance);
        return instance;
    }

    public void registerBean(Object bean) {
        registerBean(null, bean);
    }

    public void registerBean(String name, Object bean) {
        checkDestroyed();
        // avoid duplicated register same bean
        if (containsBean(name, bean)) {
            return;
        }

        Class<?> beanClass = bean.getClass();
        if (name == null) {
            name = beanClass.getName() + "#" + getNextId(beanClass);
        }
        initializeBean(name, bean);

        registeredBeanInfos.add(new BeanInfo(name, bean));
        beanCache.entrySet().removeIf(e -> e.getKey().getLeft().isAssignableFrom(beanClass));
    }

    public <T> T getOrRegisterBean(Class<T> type) {
        return getOrRegisterBean(null, type);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public <T> T getOrRegisterBean(String name, Class<T> type) {
        T bean = getBean(name, type);
        if (bean == null) {
            // lock by type
            synchronized (type) {
                bean = getBean(name, type);
                if (bean == null) {
                    bean = createAndRegisterBean(name, type);
                }
            }
        }
        registeredClasses.add(type);
        return bean;
    }

    public <T> T getOrRegisterBean(Class<T> type, Function<? super Class<T>, ? extends T> mappingFunction) {
        return getOrRegisterBean(null, type, mappingFunction);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public <T> T getOrRegisterBean(
            String name, Class<T> type, Function<? super Class<T>, ? extends T> mappingFunction) {
        T bean = getBean(name, type);
        if (bean == null) {
            // lock by type
            synchronized (type) {
                bean = getBean(name, type);
                if (bean == null) {
                    bean = mappingFunction.apply(type);
                    registerBean(name, bean);
                }
            }
        }
        return bean;
    }

    private void initializeBean(String name, Object bean) {
        checkDestroyed();
        try {
            if (bean instanceof ExtensionAccessorAware) {
                ((ExtensionAccessorAware) bean).setExtensionAccessor(extensionAccessor);
            }
            for (ExtensionPostProcessor processor : extensionPostProcessors) {
                processor.postProcessAfterInitialization(bean, name);
            }
            if (bean instanceof Initializable) {
                ((Initializable) bean).initialize(extensionAccessor);
            }
        } catch (Exception e) {
            throw new ScopeBeanException(
                    "register bean failed! name=" + name + ", type="
                            + bean.getClass().getName(),
                    e);
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private void initializeBeanDefinitions(Class<?> type) {
        for (int i = 0, size = registeredBeanDefinitions.size(); i < size; i++) {
            BeanDefinition<?> definition = registeredBeanDefinitions.get(i);
            if (definition.initialized) {
                continue;
            }

            Class<?> beanClass = definition.beanClass;
            if (!type.isAssignableFrom(beanClass)) {
                continue;
            }
            synchronized (type) {
                if (definition.initialized) {
                    continue;
                }

                Object bean;
                Supplier<?> factory = definition.beanFactory;
                if (factory == null) {
                    try {
                        bean = instantiationStrategy.instantiate(beanClass);
                    } catch (Throwable e) {
                        throw new ScopeBeanException("create bean instance failed, type=" + beanClass.getName(), e);
                    }
                } else {
                    initializeBean(definition.name, factory);
                    try {
                        bean = factory.get();
                    } catch (Exception e) {
                        throw new ScopeBeanException("create bean instance failed, type=" + beanClass.getName(), e);
                    }
                }
                registerBean(definition.name, bean);
                definition.initialized = true;
            }
        }
    }

    private boolean containsBean(String name, Object bean) {
        for (BeanInfo beanInfo : registeredBeanInfos) {
            if (beanInfo.instance == bean && (name == null || name.equals(beanInfo.name))) {
                return true;
            }
        }
        return false;
    }

    private int getNextId(Class<?> beanClass) {
        return beanNameIdCounterMap
                .computeIfAbsent(beanClass, key -> new AtomicInteger())
                .incrementAndGet();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getBeansOfType(Class<T> type) {
        initializeBeanDefinitions(type);
        List<T> currentBeans = (List<T>) registeredBeanInfos.stream()
                .filter(beanInfo -> type.isInstance(beanInfo.instance))
                .map(beanInfo -> beanInfo.instance)
                .collect(Collectors.toList());
        if (parent != null) {
            currentBeans.addAll(parent.getBeansOfType(type));
        }
        return currentBeans;
    }

    public <T> T getBean(Class<T> type) {
        return getBean(null, type);
    }

    public <T> T getBean(String name, Class<T> type) {
        T bean = getBeanFromCache(name, type);
        if (bean == null && parent != null) {
            return parent.getBean(name, type);
        }
        return bean;
    }

    @SuppressWarnings("unchecked")
    private <T> T getBeanFromCache(String name, Class<T> type) {
        Pair<Class<?>, String> key = Pair.of(type, name);
        Optional<Object> value = beanCache.get(key);
        if (value == null) {
            initializeBeanDefinitions(type);
            value = beanCache.computeIfAbsent(key, k -> {
                try {
                    return Optional.ofNullable(getBeanInternal(name, type));
                } catch (ScopeBeanException e) {
                    return Optional.of(e);
                }
            });
        }
        Object bean = value.orElse(null);
        if (bean instanceof ScopeBeanException) {
            throw (ScopeBeanException) bean;
        }
        return (T) bean;
    }

    @SuppressWarnings("unchecked")
    private <T> T getBeanInternal(String name, Class<T> type) {
        // All classes are derived from java.lang.Object, cannot filter bean by it
        if (type == Object.class) {
            return null;
        }
        List<BeanInfo> candidates = null;
        BeanInfo firstCandidate = null;
        for (BeanInfo beanInfo : registeredBeanInfos) {
            // if required bean type is same class/superclass/interface of the registered bean
            if (type.isAssignableFrom(beanInfo.instance.getClass())) {
                if (StringUtils.isEquals(beanInfo.name, name)) {
                    return (T) beanInfo.instance;
                } else {
                    // optimize for only one matched bean
                    if (firstCandidate == null) {
                        firstCandidate = beanInfo;
                    } else {
                        if (candidates == null) {
                            candidates = new ArrayList<>();
                            candidates.add(firstCandidate);
                        }
                        candidates.add(beanInfo);
                    }
                }
            }
        }

        // if bean name not matched and only single candidate
        if (candidates != null) {
            if (candidates.size() == 1) {
                return (T) candidates.get(0).instance;
            } else if (candidates.size() > 1) {
                List<String> candidateBeanNames =
                        candidates.stream().map(beanInfo -> beanInfo.name).collect(Collectors.toList());
                throw new ScopeBeanException("expected single matching bean but found " + candidates.size()
                        + " candidates for type [" + type.getName() + "]: " + candidateBeanNames);
            }
        } else if (firstCandidate != null) {
            return (T) firstCandidate.instance;
        }
        return null;
    }

    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            for (BeanInfo beanInfo : registeredBeanInfos) {
                if (beanInfo.instance instanceof Disposable) {
                    try {
                        Disposable beanInstance = (Disposable) beanInfo.instance;
                        beanInstance.destroy();
                    } catch (Throwable e) {
                        LOGGER.error(
                                CONFIG_FAILED_DESTROY_INVOKER,
                                "",
                                "",
                                "An error occurred when destroy bean [name=" + beanInfo.name + ", bean="
                                        + beanInfo.instance + "]: " + e,
                                e);
                    }
                }
            }
            registeredBeanInfos.clear();
            registeredBeanDefinitions.clear();
            beanCache.clear();
        }
    }

    public boolean isDestroyed() {
        return destroyed.get();
    }

    private void checkDestroyed() {
        if (destroyed.get()) {
            throw new IllegalStateException("ScopeBeanFactory is destroyed");
        }
    }

    static final class BeanInfo {
        private final String name;
        private final Object instance;

        BeanInfo(String name, Object instance) {
            this.name = name;
            this.instance = instance;
        }
    }

    static final class BeanDefinition<T> {

        private final String name;
        private final Class<T> beanClass;
        private final Supplier<T> beanFactory;
        private volatile boolean initialized;

        BeanDefinition(String name, Class<T> beanClass) {
            this.name = name;
            this.beanClass = beanClass;
            beanFactory = null;
        }

        BeanDefinition(String name, Class<T> beanClass, Supplier<T> beanFactory) {
            this.name = name;
            this.beanClass = beanClass;
            this.beanFactory = beanFactory;
        }
    }

    public Set<Class<?>> getRegisteredClasses() {
        return registeredClasses;
    }
}
