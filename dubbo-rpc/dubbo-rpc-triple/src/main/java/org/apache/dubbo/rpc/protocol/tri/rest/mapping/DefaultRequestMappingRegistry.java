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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.DescriptorUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestInitializeException;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RadixTree.Match;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ProducesCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.MethodWalker;
import org.apache.dubbo.rpc.protocol.tri.rest.util.PathUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.dubbo.rpc.protocol.tri.rest.Messages.AMBIGUOUS_MAPPING;
import static org.apache.dubbo.rpc.protocol.tri.rest.Messages.DUPLICATE_MAPPING;

public final class DefaultRequestMappingRegistry implements RequestMappingRegistry {

    private final List<RequestMappingResolver> resolvers;

    private final RadixTree<Registration> tree = new RadixTree<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public DefaultRequestMappingRegistry(FrameworkModel frameworkModel) {
        resolvers = frameworkModel.getActivateExtensions(RequestMappingResolver.class);
    }

    @Override
    public void register(Invoker<?> invoker) {
        Object service = invoker.getUrl().getServiceModel().getProxyObject();
        new MethodWalker().walk(service.getClass(), (classes, consumer) -> {
            for (int i = 0, size = resolvers.size(); i < size; i++) {
                RequestMappingResolver resolver = resolvers.get(i);
                RestToolKit toolKit = resolver.getRestToolKit();
                ServiceMeta serviceMeta = new ServiceMeta(classes, service, invoker.getUrl(), toolKit);
                if (!resolver.accept(serviceMeta)) {
                    continue;
                }
                RequestMapping classMapping = resolver.resolve(serviceMeta);
                consumer.accept((methods) -> {
                    MethodMeta methodMeta = new MethodMeta(methods, serviceMeta);
                    RequestMapping methodMapping = resolver.resolve(methodMeta);
                    if (methodMapping == null) {
                        return;
                    }
                    if (classMapping != null) {
                        methodMapping = classMapping.combine(methodMapping);
                    }
                    register0(methodMapping, buildHandlerMeta(invoker, methodMeta));
                });
            }
        });
    }

    private void register0(RequestMapping mapping, HandlerMeta handler) {
        lock.writeLock().lock();
        try {
            Registration registration = new Registration();
            registration.mapping = mapping;
            registration.meta = handler;
            for (PathExpression path : mapping.getPathCondition().getExpressions()) {
                Registration exists = tree.addPath(path, registration);
                if (exists != null) {
                    throw new RestInitializeException(DUPLICATE_MAPPING, path.getPath(), mapping, exists.mapping);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private HandlerMeta buildHandlerMeta(Invoker<?> invoker, MethodMeta methodMeta) {
        ServiceDescriptor serviceDescriptor = DescriptorUtils.getReflectionServiceDescriptor(invoker.getUrl());
        String serviceInterface = invoker.getUrl().getServiceInterface();
        Assert.notNull(serviceDescriptor, "ServiceDescriptor for [%s] can't be null", serviceInterface);
        Method method = methodMeta.getMethod();
        MethodDescriptor methodDescriptor = serviceDescriptor.getMethod(method.getName(), method.getParameterTypes());
        Assert.notNull(methodDescriptor, "MethodDescriptor for [%s] can't be null", method);
        return new HandlerMeta(
                invoker,
                methodMeta,
                MethodMetadata.fromMethodDescriptor(methodDescriptor),
                methodDescriptor,
                serviceDescriptor);
    }

    @Override
    public void unregister(Invoker<?> invoker) {
        lock.writeLock().lock();
        try {
            tree.remove(mapping -> mapping.meta.getInvoker() == invoker);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void destroy() {
        lock.writeLock().lock();
        try {
            tree.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public HandlerMeta lookup(HttpRequest request) {
        String path = PathUtils.normalize(request.rawPath());
        request.setAttribute(RestConstants.PATH_ATTRIBUTE, path);
        List<Match<Registration>> matches = new ArrayList<>();
        lock.readLock().lock();
        try {
            tree.match(path, matches);
        } finally {
            lock.readLock().unlock();
        }

        int size = matches.size();
        if (size == 0) {
            return null;
        }
        List<Candidate> candidates = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Match<Registration> match = matches.get(i);
            RequestMapping mapping = match.getValue().mapping.match(request, match.getExpression());
            if (mapping != null) {
                Candidate candidate = new Candidate();
                candidate.mapping = mapping;
                candidate.meta = match.getValue().meta;
                candidate.expression = match.getExpression();
                candidate.variableMap = match.getVariableMap();
                candidates.add(candidate);
            }
        }

        size = candidates.size();
        if (size == 0) {
            return null;
        }
        if (size > 1) {
            candidates.sort((c1, c2) -> {
                int comparison = c1.expression.compareTo(c2.expression, path);
                if (comparison != 0) {
                    return comparison;
                }
                comparison = c1.mapping.compareTo(c2.mapping, request);
                if (comparison != 0) {
                    return comparison;
                }
                return c1.variableMap.size() - c2.variableMap.size();
            });
            Candidate first = candidates.get(0);
            Candidate second = candidates.get(1);
            if (first.mapping.compareTo(second.mapping, request) == 0) {
                throw new RestInitializeException(AMBIGUOUS_MAPPING, path, first.mapping, second.mapping);
            }
        }

        Candidate winner = candidates.get(0);
        RequestMapping mapping = winner.mapping;
        HandlerMeta handler = winner.meta;
        request.setAttribute(RestConstants.MAPPING_ATTRIBUTE, mapping);
        request.setAttribute(RestConstants.HANDLER_ATTRIBUTE, handler);

        if (!winner.variableMap.isEmpty()) {
            request.setAttribute(RestConstants.URI_TEMPLATE_VARIABLES_ATTRIBUTE, winner.variableMap);
        }

        ProducesCondition producesCondition = mapping.getProducesCondition();
        if (producesCondition != null) {
            request.setAttribute(RestConstants.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, producesCondition.getMediaTypes());
        }

        return handler;
    }

    @Override
    public boolean exists(String path, String method) {
        List<Match<Registration>> matches = new ArrayList<>();
        lock.readLock().lock();
        try {
            tree.match(path, matches);
        } finally {
            lock.readLock().unlock();
        }
        for (int i = 0, size = matches.size(); i < size; i++) {
            if (matches.get(i).getValue().mapping.matchMethod(method)) {
                return true;
            }
        }
        return false;
    }

    private static final class Registration {
        RequestMapping mapping;
        HandlerMeta meta;

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != Registration.class) {
                return false;
            }
            return mapping.equals(((Registration) obj).mapping);
        }

        @Override
        public int hashCode() {
            return mapping.hashCode();
        }
    }

    private static final class Candidate {
        RequestMapping mapping;
        HandlerMeta meta;
        PathExpression expression;
        Map<String, String> variableMap;
    }
}
