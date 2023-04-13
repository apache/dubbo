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

package org.apache.dubbo.metrics.metadata.event;

import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.listener.MetricsLifeListener;

import java.util.function.BiConsumer;

public abstract class MetadataListener implements MetricsLifeListener<MetadataEvent> {

    private final Object enumType;

    public MetadataListener(Object enumType) {
        this.enumType = enumType;
    }

    @Override
    public boolean isSupport(MetricsEvent event) {
        return event.isAvailable() && event.isAssignableFrom(enumType);
    }

    static <T> MetadataListener onEvent(T enumType, BiConsumer<MetadataEvent, T> postFunc) {

        return new MetadataListener(enumType) {
            @Override
            public void onEvent(MetadataEvent event) {
                postFunc.accept(event, enumType);
            }
        };
    }

    static <T> MetadataListener onFinish(T enumType, BiConsumer<MetadataEvent, T> finishFunc) {

        return new MetadataListener(enumType) {
            @Override
            public void onEventFinish(MetadataEvent event) {
                finishFunc.accept(event, enumType);
            }
        };
    }

    static <T> MetadataListener onError(T enumType, BiConsumer<MetadataEvent, T> errorFunc) {

        return new MetadataListener(enumType) {
            @Override
            public void onEventError(MetadataEvent event) {
                errorFunc.accept(event, enumType);
            }
        };
    }
}
