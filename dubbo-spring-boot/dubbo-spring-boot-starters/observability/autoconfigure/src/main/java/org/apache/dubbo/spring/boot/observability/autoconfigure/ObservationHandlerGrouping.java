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
package org.apache.dubbo.spring.boot.observability.autoconfigure;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;

/**
 * Groups {@link ObservationHandler ObservationHandlers} by type.
 * copy from {@link org.springframework.boot.actuate.autoconfigure.observation.ObservationHandlerGrouping}
 * this class is available starting from Boot 3.0. It's not available if you're using Boot < 3.0
 *
 * @author Andy Wilkinson
 */
class ObservationHandlerGrouping {

    private final List<Class<? extends ObservationHandler>> categories;

    ObservationHandlerGrouping(Class<? extends ObservationHandler> category) {
        this(Collections.singletonList(category));
    }

    ObservationHandlerGrouping(List<Class<? extends ObservationHandler>> categories) {
        this.categories = categories;
    }

    void apply(List<ObservationHandler<?>> handlers, ObservationRegistry.ObservationConfig config) {
        MultiValueMap<Class<? extends ObservationHandler>, ObservationHandler<?>> groupings = new LinkedMultiValueMap<>();
        for (ObservationHandler<?> handler : handlers) {
            Class<? extends ObservationHandler> category = findCategory(handler);
            if (category != null) {
                groupings.add(category, handler);
            } else {
                config.observationHandler(handler);
            }
        }
        for (Class<? extends ObservationHandler> category : this.categories) {
            List<ObservationHandler<?>> handlerGroup = groupings.get(category);
            if (!CollectionUtils.isEmpty(handlerGroup)) {
                config.observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(handlerGroup));
            }
        }
    }

    private Class<? extends ObservationHandler> findCategory(ObservationHandler<?> handler) {
        for (Class<? extends ObservationHandler> category : this.categories) {
            if (category.isInstance(handler)) {
                return category;
            }
        }
        return null;
    }

}
