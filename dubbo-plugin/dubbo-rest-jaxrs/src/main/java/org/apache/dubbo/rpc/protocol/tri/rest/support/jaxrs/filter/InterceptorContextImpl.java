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
package org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.filter;

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;

import javax.ws.rs.ext.InterceptorContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

public abstract class InterceptorContextImpl implements InterceptorContext {

    protected final HttpRequest request;

    public InterceptorContextImpl(HttpRequest request) {
        this.request = request;
    }

    @Override
    public Object getProperty(String name) {
        return request.attribute(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return request.parameterNames();
    }

    @Override
    public void setProperty(String name, Object object) {
        request.setAttribute(name, object);
    }

    @Override
    public void removeProperty(String name) {
        request.removeAttribute(name);
    }

    @Override
    public Annotation[] getAnnotations() {
        return getHandler().getMethod().getRawAnnotations();
    }

    @Override
    public void setAnnotations(Annotation[] annotations) {}

    @Override
    public Class<?> getType() {
        return getHandler().getMethod().getReturnType();
    }

    @Override
    public void setType(Class<?> type) {}

    @Override
    public Type getGenericType() {
        return getHandler().getMethod().getGenericReturnType();
    }

    @Override
    public void setGenericType(Type genericType) {}

    private HandlerMeta getHandler() {
        return request.attribute(RestConstants.HANDLER_ATTRIBUTE);
    }
}
