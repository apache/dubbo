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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Pair;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

@SuppressWarnings({"rawtypes"})
public class ParamConverterFactory {

    public static final Logger logger = LoggerFactory.getLogger(ParamConverterFactory.class);
    private final Map<Pair<Pair<Class<?>, Type>, Annotation[]>, Optional<ParamConverter>> cache =
            CollectionUtils.newConcurrentHashMap();
    private final List<ParamConverterProvider> providers = new ArrayList<>();

    ParamConverterFactory() {
        ServiceLoader<ParamConverterProvider> serviceLoader = ServiceLoader.load(ParamConverterProvider.class);
        Iterator<ParamConverterProvider> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            try {
                ParamConverterProvider paramConverterProvider = iterator.next();
                providers.add(paramConverterProvider);
            } catch (Throwable e) {
                logger.error("Spi Fail to load :" + e.getMessage());
            }
        }
    }

    public <T> Optional<ParamConverter> getParamConverter(
            Class<T> rawType, Type genericType, Annotation[] annotations) {
        Pair<Pair<Class<?>, Type>, Annotation[]> pair = Pair.of(Pair.of(rawType, genericType), annotations);
        return cache.computeIfAbsent(pair, k -> {
            for (ParamConverterProvider provider : providers) {
                ParamConverter converter = provider.getConverter(rawType, genericType, annotations);
                if (converter != null) {
                    return Optional.of(converter);
                }
            }
            return Optional.empty();
        });
    }
}
