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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Pair;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

@SuppressWarnings({"rawtypes"})
public class ParamConverterFactory {

    private final Map<Pair<Pair<Class<?>, Type>, Annotation[]>, ParamConverter> cache =
            CollectionUtils.newConcurrentHashMap();
    private final List<ParamConverterProvider> providers = new ArrayList<>();

    ParamConverterFactory() {
        ServiceLoader.load(ParamConverterProvider.class).forEach(providers::add);
    }

    public <T> ParamConverter getParamConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        Pair<Pair<Class<?>, Type>, Annotation[]> pair = Pair.of(Pair.of(rawType, genericType), annotations);
        ParamConverter paramConverter = cache.get(pair);
        if (paramConverter != null) {
            return paramConverter;
        }
        for (ParamConverterProvider provider : providers) {
            paramConverter = provider.getConverter(rawType, genericType, annotations);
            if (paramConverter != null) {
                cache.put(pair, paramConverter);
                return paramConverter;
            }
        }
        return null;
    }
}
