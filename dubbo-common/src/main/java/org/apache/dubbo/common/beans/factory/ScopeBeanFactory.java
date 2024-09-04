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
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ScopeModelAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_DESTROY_INVOKER;

/**
 * A bean factory for internal sharing.
 */
public class ScopeBeanFactory {

    protected static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(ScopeBeanFactory.class);

    private final ScopeBeanFactory parent;
    private final ExtensionAccessor extensionAccessor;
    private final List<ExtensionPostProcessor> extensionPostProcessors;
    private final ConcurrentHashMap<Class<?>, AtomicInteger> beanNameIdCounterMap = new ConcurrentHashMap<>();
    private final List<BeanInfo> registeredBeanInfos = new CopyOnWriteArrayList<>();
    private InstantiationStrategy instantiationStrategy;
    private final AtomicBoolean destroyed = new AtomicBoolean();
    private final Set<Class<?>> registeredClasses = new ConcurrentHashSet<>();
    /*
     * Bean creation and registration thread-safe control, even across scope-model.
     */
    private static final Map<Class<?>, ReentrantLock> typeLocks = new ConcurrentHashMap<>();

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

    public <T> T registerBean(Class<T> bean) throws ScopeBeanException {
        return this.getOrRegisterBean(null, bean);
    }

    public <T> T registerBean(String name, Class<T> clazz) throws ScopeBeanException {
        return getOrRegisterBean(name, clazz);
    }

    public void registerBean(Object bean) {
        this.registerBean(null, bean);
    }

    public void registerBean(String name, Object bean) {
        this.registerBean(name, bean.getClass(), bean);
    }

    public void registerBean(String name, Class<?> type, Object bean) {
        checkDestroyed();
        // avoid duplicated register same bean
        if (containsBean(name, bean)) {
            return;
        }

        if (name == null) {
            name = type.getName() + "#" + getNextId(type);
        }
        initializeBean(name, bean);

        registeredBeanInfos.add(new BeanInfo(name, bean));
        registeredClasses.add(type);
    }

    public <T> T getOrRegisterBean(Class<T> type) {
        return getOrRegisterBean(null, type);
    }

    public <T> T getOrRegisterBean(String name, final Class<T> type) {
        Function<? super Class<T>, ? extends T> func = (Class<T> k) -> {
            try {
                return instantiationStrategy.instantiate(k);
            } catch (Exception e) {
                throw new ScopeBeanException("create bean instance failed, type=" + type.getName(), e);
            }
        };
        return getOrRegisterBean(name, type, func);
    }

    public <T> T getOrRegisterBean(Class<T> type, Function<? super Class<T>, ? extends T> mappingFunction) {
        return getOrRegisterBean(null, type, mappingFunction);
    }

    public <T> T getOrRegisterBean(
            String name, Class<T> type, Function<? super Class<T>, ? extends T> mappingFunction) {
        T bean = getBean(name, type);
        if (bean == null) {
            typeLocks.putIfAbsent(type, new ReentrantLock());
            ReentrantLock typeLock = typeLocks.get(type);
            // lock by type
            typeLock.lock();
            try {
                bean = getBean(name, type);
                if (bean == null) {
                    bean = mappingFunction.apply(type);
                    registerBean(name, type, bean);
                }
            } finally {
                typeLock.unlock();
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
        } catch (Exception e) {
            throw new ScopeBeanException(
                    "register bean failed! name=" + name + ", type="
                            + bean.getClass().getName(),
                    e);
        }
    }

    private boolean containsBean(String name, Object bean) {
        for (BeanInfo beanInfo : registeredBeanInfos) {
            if (beanInfo.instance == bean && (name == null || StringUtils.isEquals(name, beanInfo.name))) {
                return true;
            }
        }
        return false;
    }

    private int getNextId(Class<?> beanClass) {
        return ConcurrentHashMapUtils.computeIfAbsent(beanNameIdCounterMap, beanClass, key -> new AtomicInteger())
                .incrementAndGet();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getBeansOfType(Class<T> type) {
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
        return this.getBean(null, type);
    }

    public <T> T getBean(String name, Class<T> type) {
        T bean = getBeanInternal(name, type);
        if (bean == null && parent != null) {
            return parent.getBean(name, type);
        }
        return bean;
    }

    @SuppressWarnings("unchecked")
    private <T> T getBeanInternal(String name, Class<T> type) {
        checkDestroyed();
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

    static class BeanInfo {
        private final String name;
        private final Object instance;

        public BeanInfo(String name, Object instance) {
            this.name = name;
            this.instance = instance;
        }
    }

    public Set<Class<?>> getRegisteredClasses() {
        return registeredClasses;
    }
}
