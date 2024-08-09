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
package org.apache.dubbo.common.json;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.TypeUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class CompositeJsonUtilCustomizer implements JsonUtilCustomizer<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeJsonUtilCustomizer.class);

    private final List<JsonUtilCustomizer> jsonUtilCustomizers;
    private final Map<Class, List<JsonUtilCustomizer>> cache = CollectionUtils.newConcurrentHashMap();

    public CompositeJsonUtilCustomizer(FrameworkModel frameworkModel) {
        jsonUtilCustomizers = frameworkModel.getActivateExtensions(JsonUtilCustomizer.class);
    }

    public boolean isAvailable() {
        return !jsonUtilCustomizers.isEmpty();
    }

    @Override
    public void customize(Object target) {
        List<JsonUtilCustomizer> customizers = getSuitableJsonUtilCustomizers(target.getClass());
        for (int i = 0, len = customizers.size(); i < len; i++) {
            customizers.get(i).customize(target);
        }
    }

    private List<JsonUtilCustomizer> getSuitableJsonUtilCustomizers(Class type) {
        return cache.computeIfAbsent(type, k -> {
            List<JsonUtilCustomizer> result = new ArrayList<>();
            for (JsonUtilCustomizer customizer : jsonUtilCustomizers) {
                Class<?> supportType = TypeUtils.getSuperGenericType(customizer.getClass(), 0);
                if (supportType != null && supportType.isAssignableFrom(type)) {
                    result.add(customizer);
                }
            }
            if (result.isEmpty()) {
                return Collections.emptyList();
            }
            LOGGER.info("Found suitable JsonUtilCustomizer for [{}], customizers: {}", type, result);
            return result;
        });
    }
}
