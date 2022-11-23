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
package org.apache.dubbo.common.beans.support;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelAccessor;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface to create instance for specify type, using both in {@link ExtensionLoader} and {@link ScopeBeanFactory}.
 */
public class InstantiationStrategy {

    private final ScopeModelAccessor scopeModelAccessor;

    public InstantiationStrategy() {
        this(null);
    }

    public InstantiationStrategy(ScopeModelAccessor scopeModelAccessor) {
        this.scopeModelAccessor = scopeModelAccessor;
    }

    @SuppressWarnings("unchecked")
    public <T> T instantiate(Class<T> type) throws ReflectiveOperationException {

        // should not use default constructor directly, maybe also has another constructor matched scope model arguments
        // 1. try to get default constructor
        Constructor<T> defaultConstructor = null;
        try {
            defaultConstructor = type.getConstructor();
        } catch (NoSuchMethodException e) {
            // ignore no default constructor
        }

        // 2. use matched constructor if found
        List<Constructor<?>> matchedConstructors = new ArrayList<>();
        Constructor<?>[] declaredConstructors = type.getConstructors();
        for (Constructor<?> constructor : declaredConstructors) {
            if (isMatched(constructor)) {
                matchedConstructors.add(constructor);
            }
        }
        // remove default constructor from matchedConstructors
        if (defaultConstructor != null) {
            matchedConstructors.remove(defaultConstructor);
        }

        // match order:
        // 1. the only matched constructor with parameters
        // 2. default constructor if absent

        Constructor<?> targetConstructor;
        if (matchedConstructors.size() > 1) {
            throw new IllegalArgumentException("Expect only one but found " +
                matchedConstructors.size() + " matched constructors for type: " + type.getName() +
                ", matched constructors: " + matchedConstructors);
        } else if (matchedConstructors.size() == 1) {
            targetConstructor = matchedConstructors.get(0);
        } else if (defaultConstructor != null) {
            targetConstructor = defaultConstructor;
        } else {
            throw new IllegalArgumentException("None matched constructor was found for type: " + type.getName());
        }

        // create instance with arguments
        Class<?>[] parameterTypes = targetConstructor.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            args[i] = getArgumentValueForType(parameterTypes[i]);
        }
        return (T) targetConstructor.newInstance(args);
    }

    private boolean isMatched(Constructor<?> constructor) {
        for (Class<?> parameterType : constructor.getParameterTypes()) {
            if (!isSupportedConstructorParameterType(parameterType)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSupportedConstructorParameterType(Class<?> parameterType) {
        return ScopeModel.class.isAssignableFrom(parameterType);
    }

    private Object getArgumentValueForType(Class<?> parameterType) {
        // get scope mode value
        if (scopeModelAccessor != null) {
            if (parameterType == ScopeModel.class) {
                return scopeModelAccessor.getScopeModel();
            } else if (parameterType == FrameworkModel.class) {
                return scopeModelAccessor.getFrameworkModel();
            } else if (parameterType == ApplicationModel.class) {
                return scopeModelAccessor.getApplicationModel();
            } else if (parameterType == ModuleModel.class) {
                return scopeModelAccessor.getModuleModel();
            }
        }
        return null;
    }

}
