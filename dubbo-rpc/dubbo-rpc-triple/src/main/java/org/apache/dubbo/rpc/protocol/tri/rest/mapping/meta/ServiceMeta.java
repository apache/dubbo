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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.rest.util.PathUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ServiceMeta extends AnnotationSupport {

    private final List<Class<?>> hierarchy;
    private final Class<?> type;
    private final Object service;
    private final ServiceDescriptor serviceDescriptor;
    private final URL url;
    private final String contextPath;

    private List<MethodMeta> exceptionHandlers;

    public ServiceMeta(
            Collection<Class<?>> hierarchy,
            ServiceDescriptor serviceDescriptor,
            Object service,
            URL url,
            RestToolKit toolKit) {
        super(toolKit);
        this.hierarchy = new ArrayList<>(hierarchy);
        this.serviceDescriptor = serviceDescriptor;
        type = this.hierarchy.get(0);
        this.service = service;
        this.url = url;
        contextPath = PathUtils.getContextPath(url);
    }

    public List<Class<?>> getHierarchy() {
        return hierarchy;
    }

    public Class<?> getType() {
        return type;
    }

    public ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    public Object getService() {
        return service;
    }

    public URL getUrl() {
        return url;
    }

    public String getServiceInterface() {
        return url.getServiceInterface();
    }

    public String getServiceGroup() {
        return url.getGroup();
    }

    public String getServiceVersion() {
        return url.getVersion();
    }

    public String getContextPath() {
        return contextPath;
    }

    public List<MethodMeta> getExceptionHandlers() {
        return exceptionHandlers;
    }

    @Override
    public List<? extends AnnotatedElement> getAnnotatedElements() {
        return hierarchy;
    }

    @Override
    protected AnnotatedElement getAnnotatedElement() {
        return hierarchy.get(0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("ServiceMeta{interface=")
                .append(getServiceInterface())
                .append(", service=")
                .append(toShortString());
        if (StringUtils.isNotEmpty(contextPath)) {
            sb.append(", contextPath='").append(contextPath).append('\'');
        }
        String group = getServiceGroup();
        if (StringUtils.isNotEmpty(group)) {
            sb.append(", group='").append(group).append('\'');
        }
        String version = getServiceVersion();
        if (StringUtils.isNotEmpty(version)) {
            sb.append(", version='").append(version).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }

    public String toShortString() {
        return type.getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(service));
    }

    public void addExceptionHandler(MethodMeta methodMeta) {
        if (exceptionHandlers == null) {
            exceptionHandlers = new ArrayList<>();
        }
        exceptionHandlers.add(methodMeta);
    }
}
