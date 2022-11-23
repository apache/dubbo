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

package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.TypeUtils;
import org.apache.dubbo.rpc.cluster.Merger;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_LOAD_MERGER;

public class MergerFactory implements ScopeModelAware {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MergerFactory.class);

    private ConcurrentMap<Class<?>, Merger<?>> MERGER_CACHE = new ConcurrentHashMap<Class<?>, Merger<?>>();
    private ScopeModel scopeModel;

    @Override
    public void setScopeModel(ScopeModel scopeModel) {
        this.scopeModel = scopeModel;
    }

    /**
     * Find the merger according to the returnType class, the merger will
     * merge an array of returnType into one
     *
     * @param returnType the merger will return this type
     * @return the merger which merges an array of returnType into one, return null if not exist
     * @throws IllegalArgumentException if returnType is null
     */
    public <T> Merger<T> getMerger(Class<T> returnType) {
        if (returnType == null) {
            throw new IllegalArgumentException("returnType is null");
        }

        if (CollectionUtils.isEmptyMap(MERGER_CACHE)) {
            loadMergers();
        }
        Merger merger = MERGER_CACHE.get(returnType);
        if (merger == null && returnType.isArray()) {
            merger = ArrayMerger.INSTANCE;
        }
        return merger;
    }

    private void loadMergers() {
        Set<String> names = scopeModel.getExtensionLoader(Merger.class)
            .getSupportedExtensions();
        for (String name : names) {
            Merger m = scopeModel.getExtensionLoader(Merger.class).getExtension(name);
            Class<?> actualTypeArg = getActualTypeArgument(m.getClass());
            if (actualTypeArg == null) {
                logger.warn(CLUSTER_FAILED_LOAD_MERGER,"load merger config failed","","Failed to get actual type argument from merger " + m.getClass().getName());
                continue;
            }
            MERGER_CACHE.putIfAbsent(actualTypeArg, m);
        }
    }

    /**
     * get merger's actual type argument (same as return type)
     * @param mergerCls
     * @return
     */
    private Class<?> getActualTypeArgument(Class<? extends Merger> mergerCls) {
        Class<?> superClass = mergerCls;
        while (superClass != Object.class) {
            Type[] interfaceTypes = superClass.getGenericInterfaces();
            ParameterizedType mergerType;
            for (Type it : interfaceTypes) {
                if (it instanceof ParameterizedType
                    && (mergerType = ((ParameterizedType) it)).getRawType() == Merger.class) {
                    Type typeArg = mergerType.getActualTypeArguments()[0];
                    return TypeUtils.getRawClass(typeArg);
                }
            }

            superClass = superClass.getSuperclass();
        }

        return null;
    }
}
