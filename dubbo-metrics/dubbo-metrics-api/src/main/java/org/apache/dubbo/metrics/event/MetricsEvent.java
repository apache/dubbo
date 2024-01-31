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
package org.apache.dubbo.metrics.event;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.metrics.exception.MetricsNeverHappenException;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * BaseMetricsEvent.
 */
public abstract class MetricsEvent {

    /**
     * Metric object. (eg. {@link MethodMetric})
     */
    protected final transient ApplicationModel source;

    private boolean available = true;
    private final TypeWrapper typeWrapper;
    private final String appName;
    private final MetricsDispatcher metricsDispatcher;

    private final Map<String, Object> attachments = new IdentityHashMap<>(8);

    public MetricsEvent(ApplicationModel source, TypeWrapper typeWrapper) {
        this(source, null, null, typeWrapper);
    }

    public MetricsEvent(
            ApplicationModel source, String appName, MetricsDispatcher metricsDispatcher, TypeWrapper typeWrapper) {
        this.typeWrapper = typeWrapper;
        if (source == null) {
            this.source = ApplicationModel.defaultModel();
            // Appears only in unit tests
            this.available = false;
        } else {
            this.source = source;
        }
        if (metricsDispatcher == null) {
            if (this.source.isDestroyed()) {
                this.metricsDispatcher = null;
            } else {
                ScopeBeanFactory beanFactory = this.source.getBeanFactory();
                if (beanFactory.isDestroyed()) {
                    this.metricsDispatcher = null;
                } else {
                    MetricsDispatcher dispatcher = beanFactory.getBean(MetricsDispatcher.class);
                    this.metricsDispatcher = dispatcher;
                }
            }
        } else {
            this.metricsDispatcher = metricsDispatcher;
        }
        if (appName == null) {
            this.appName = this.source.tryGetApplicationName();
        } else {
            this.appName = appName;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachmentValue(String key) {
        if (key == null) {
            throw new MetricsNeverHappenException("Attachment key is null");
        }
        return (T) attachments.get(key);
    }

    public Map<String, Object> getAttachments() {
        return Collections.unmodifiableMap(attachments);
    }

    public void putAttachment(String key, Object value) {
        attachments.put(key, value);
    }

    public void putAttachments(Map<String, String> attachments) {
        this.attachments.putAll(attachments);
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public void customAfterPost(Object postResult) {}

    public ApplicationModel getSource() {
        return source;
    }

    public MetricsDispatcher getMetricsDispatcher() {
        return metricsDispatcher;
    }

    public String appName() {
        return appName;
    }

    public TypeWrapper getTypeWrapper() {
        return typeWrapper;
    }

    public boolean isAssignableFrom(Object type) {
        return typeWrapper.isAssignableFrom(type);
    }

    public String toString() {
        return getClass().getName() + "[source=" + source + "]";
    }

    public enum Type {
        TOTAL("TOTAL_%s"),
        SUCCEED("SUCCEED_%s"),
        BUSINESS_FAILED("BUSINESS_FAILED_%s"),
        REQUEST_TIMEOUT("REQUEST_TIMEOUT_%s"),
        REQUEST_LIMIT("REQUEST_LIMIT_%s"),
        PROCESSING("PROCESSING_%s"),
        UNKNOWN_FAILED("UNKNOWN_FAILED_%s"),
        TOTAL_FAILED("TOTAL_FAILED_%s"),
        APPLICATION_INFO("APPLICATION_INFO_%s"),
        NETWORK_EXCEPTION("NETWORK_EXCEPTION_%s"),
        SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE_%s"),
        CODEC_EXCEPTION("CODEC_EXCEPTION_%s"),
        NO_INVOKER_AVAILABLE("NO_INVOKER_AVAILABLE_%s"),
        ;

        private final String name;

        public final String getName() {
            return this.name;
        }

        public final String getNameByType(String type) {
            return String.format(name, type);
        }

        Type(String name) {
            this.name = name;
        }
    }
}
