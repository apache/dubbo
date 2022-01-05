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
package org.apache.dubbo.rpc.model;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.extension.ExtensionAccessor;
import org.apache.dubbo.common.extension.ExtensionDirector;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ScopeModel implements ExtensionAccessor {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ScopeModel.class);

    /**
     * The internal id is used to represent the hierarchy of the model tree, such as:
     * <ol>
     *     <li>1</li>
     *     FrameworkModel (index=1)
     *     <li>1.2</li>
     *     FrameworkModel (index=1) -> ApplicationModel (index=2)
     *     <li>1.2.0</li>
     *     FrameworkModel (index=1) -> ApplicationModel (index=2) -> ModuleModel (index=0, internal module)
     *     <li>1.2.1</li>
     *     FrameworkModel (index=1) -> ApplicationModel (index=2) -> ModuleModel (index=1, first user module)
     * </ol>
     */
    private String internalId;

    /**
     * Public Model Name, can be set from user
     */
    private String modelName;

    private String desc;

    private Set<ClassLoader> classLoaders;

    private final ScopeModel parent;
    private final ExtensionScope scope;

    private ExtensionDirector extensionDirector;

    private ScopeBeanFactory beanFactory;
    private List<ScopeModelDestroyListener> destroyListeners;

    private Map<String, Object> attributes;
    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    protected boolean internalModule;

    public ScopeModel(ScopeModel parent, ExtensionScope scope) {
        this.parent = parent;
        this.scope = scope;
    }

    /**
     * NOTE:
     * <ol>
     *  <li>The initialize method only be called in subclass.</li>
     * <li>
     * In subclass, the extensionDirector and beanFactory are available in initialize but not available in constructor.
     * </li>
     * </ol>
     */
    protected void initialize() {
        this.extensionDirector = new ExtensionDirector(parent != null ? parent.getExtensionDirector() : null, scope, this);
        this.extensionDirector.addExtensionPostProcessor(new ScopeModelAwareExtensionProcessor(this));
        this.beanFactory = new ScopeBeanFactory(parent != null ? parent.getBeanFactory() : null, extensionDirector);
        this.destroyListeners = new LinkedList<>();
        this.attributes = new ConcurrentHashMap<>();
        this.classLoaders = new ConcurrentHashSet<>();

        // Add Framework's ClassLoader by default
        ClassLoader dubboClassLoader = ScopeModel.class.getClassLoader();
        if (dubboClassLoader != null) {
            this.addClassLoader(dubboClassLoader);
        }
    }

    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            try {
                onDestroy();
                HashSet<ClassLoader> copyOfClassLoaders = new HashSet<>(classLoaders);
                for (ClassLoader classLoader : copyOfClassLoaders) {
                    removeClassLoader(classLoader);
                }
                if (beanFactory != null) {
                    beanFactory.destroy();
                }
                if (extensionDirector != null) {
                    extensionDirector.destroy();
                }
            } catch (Throwable t) {
                LOGGER.error("Error happened when destroying ScopeModel.", t);
            }
        }
    }

    public boolean isDestroyed() {
        return destroyed.get();
    }

    protected void notifyDestroy() {
        for (ScopeModelDestroyListener destroyListener : destroyListeners) {
            destroyListener.onDestroy(this);
        }
    }

    protected abstract void onDestroy();

    public final void addDestroyListener(ScopeModelDestroyListener listener) {
        destroyListeners.add(listener);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public <T> T getAttribute(String key, Class<T> type) {
        return (T) attributes.get(key);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public ExtensionDirector getExtensionDirector() {
        return extensionDirector;
    }

    public ScopeBeanFactory getBeanFactory() {
        return beanFactory;
    }

    public ScopeModel getParent() {
        return parent;
    }

    public ExtensionScope getScope() {
        return scope;
    }

    public void addClassLoader(ClassLoader classLoader) {
        this.classLoaders.add(classLoader);
        if (parent != null) {
            parent.addClassLoader(classLoader);
        }
        extensionDirector.removeAllCachedLoader();
    }

    public void removeClassLoader(ClassLoader classLoader) {
        if (checkIfClassLoaderCanRemoved(classLoader)) {
            this.classLoaders.remove(classLoader);
            if (parent != null) {
                parent.removeClassLoader(classLoader);
            }
            extensionDirector.removeAllCachedLoader();
        }
    }

    protected boolean checkIfClassLoaderCanRemoved(ClassLoader classLoader) {
        return classLoader != null && !classLoader.equals(ScopeModel.class.getClassLoader());
    }

    public Set<ClassLoader> getClassLoaders() {
        return Collections.unmodifiableSet(classLoaders);
    }

    public abstract Environment getModelEnvironment();

    public String getInternalId() {
        return this.internalId;
    }

    void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    protected String buildInternalId(String parentInternalId, long childIndex) {
        // FrameworkModel    1
        // ApplicationModel  1.1
        // ModuleModel       1.1.1
        if (StringUtils.hasText(parentInternalId)) {
            return parentInternalId + "." + childIndex;
        } else {
            return "" + childIndex;
        }
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
        this.desc = buildDesc();
    }

    public boolean isInternal() {
        return internalModule;
    }

    /**
     * @return the describe string of this scope model
     */
    public String getDesc() {
        if (this.desc == null) {
            this.desc = buildDesc();
        }
        return this.desc;
    }

    private String buildDesc() {
        // Dubbo Framework[1]
        // Dubbo Application[1.1](appName)
        // Dubbo Module[1.1.1](appName/moduleName)
        String type = this.getClass().getSimpleName().replace("Model", "");
        String desc = "Dubbo " + type + "[" + this.getInternalId() + "]";

        // append model name path
        String modelNamePath = this.getModelNamePath();
        if (StringUtils.hasText(modelNamePath)) {
            desc += "(" + modelNamePath + ")";
        }
        return desc;
    }

    private String getModelNamePath() {
        if (this instanceof ApplicationModel) {
            return safeGetAppName((ApplicationModel) this);
        } else if (this instanceof ModuleModel) {
            String modelName = this.getModelName();
            if (StringUtils.hasText(modelName)) {
                // appName/moduleName
                return safeGetAppName(((ModuleModel) this).getApplicationModel()) + "/" + modelName;
            }
        }
        return null;
    }

    private static String safeGetAppName(ApplicationModel applicationModel) {
        String modelName = applicationModel.getModelName();
        if (StringUtils.isBlank(modelName)) {
            modelName = "unknown"; // unknown application
        }
        return modelName;
    }

    @Override
    public String toString() {
        return getDesc();
    }
}
