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
package org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.utils.CollectionUtils;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_ERROR_LOAD_EXTENSION;
import static org.apache.dubbo.common.logger.LoggerFactory.getErrorTypeAwareLogger;

final class ParamConverterFactory {

    private static final ErrorTypeAwareLogger logger = getErrorTypeAwareLogger(ParamConverterFactory.class);
    private final Map<List<Object>, Optional<ParamConverter<?>>> cache = CollectionUtils.newConcurrentHashMap();
    private final List<ParamConverterProvider> providers = new ArrayList<>();

    ParamConverterFactory() {
        Iterator<ParamConverterProvider> it =
                ServiceLoader.load(ParamConverterProvider.class).iterator();
        while (it.hasNext()) {
            try {
                providers.add(it.next());
            } catch (Throwable t) {
                logger.error(COMMON_ERROR_LOAD_EXTENSION, "", "", "Failed to load ParamConverterProvider", t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getParamConverter(Class<T> type, Type genericType, Annotation[] annotations) {
        if (providers.isEmpty()) {
            return null;
        }
        List<Object> key = new ArrayList<>(annotations.length + 2);
        key.add(type);
        key.add(genericType);
        Collections.addAll(key, annotations);
        return (ParamConverter<T>) cache.computeIfAbsent(key, k -> {
                    for (ParamConverterProvider provider : providers) {
                        ParamConverter<T> converter = provider.getConverter(type, genericType, annotations);
                        if (converter != null) {
                            return Optional.of(converter);
                        }
                    }
                    return Optional.empty();
                })
                .orElse(null);
    }
}
