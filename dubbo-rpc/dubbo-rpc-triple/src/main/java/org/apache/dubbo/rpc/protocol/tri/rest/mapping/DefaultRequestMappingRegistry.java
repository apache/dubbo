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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.FluentLogger;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.RestConfig;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.remoting.http12.rest.OpenAPIService;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ReflectionMethodDescriptor;
import org.apache.dubbo.rpc.model.ReflectionServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.DescriptorUtils;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestMappingException;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RadixTree.Match;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ProducesCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.KeyString;
import org.apache.dubbo.rpc.protocol.tri.rest.util.MethodWalker;
import org.apache.dubbo.rpc.protocol.tri.rest.util.PathUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class DefaultRequestMappingRegistry implements RequestMappingRegistry {

    private static final FluentLogger LOGGER = FluentLogger.of(DefaultRequestMappingRegistry.class);

    private final FrameworkModel frameworkModel;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean initialized = new AtomicBoolean();

    private ContentNegotiator contentNegotiator;
    private OpenAPIService openAPIService;
    private List<RequestMappingResolver> resolvers;
    private RestConfig restConfig;
    private RadixTree<Registration> tree;

    public DefaultRequestMappingRegistry(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    private void init(Invoker<?> invoker) {
        contentNegotiator = frameworkModel.getOrRegisterBean(ContentNegotiator.class);
        if (TripleProtocol.OPENAPI_ENABLED) {
            openAPIService = frameworkModel.getBean(OpenAPIService.class);
        }
        resolvers = frameworkModel.getActivateExtensions(RequestMappingResolver.class);
        restConfig = ConfigManager.getProtocolOrDefault(invoker.getUrl())
                .getTripleOrDefault()
                .getRestOrDefault();
        for (RequestMappingResolver resolver : resolvers) {
            resolver.setRestConfig(restConfig);
        }
        tree = new RadixTree<>(restConfig.getCaseSensitiveMatchOrDefault());
    }

    @Override
    public void register(Invoker<?> invoker) {
        if (tree == null) {
            lock.writeLock().lock();
            try {
                if (initialized.compareAndSet(false, true)) {
                    init(invoker);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        URL url = invoker.getUrl();
        Object service = url.getServiceModel().getProxyObject();
        ServiceDescriptor sd = DescriptorUtils.getReflectionServiceDescriptor(url);
        if (sd == null) {
            return;
        }
        AtomicInteger counter = new AtomicInteger();
        long start = System.currentTimeMillis();
        new MethodWalker().walk(service.getClass(), (classes, consumer) -> {
            for (int i = 0, size = resolvers.size(); i < size; i++) {
                RequestMappingResolver resolver = resolvers.get(i);
                ServiceMeta serviceMeta = new ServiceMeta(classes, sd, service, url, resolver.getRestToolKit());
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "{} resolving rest mappings for {} at url [{}]",
                            resolver.getClass().getSimpleName(),
                            serviceMeta,
                            url.toString(""));
                }
                if (!resolver.accept(serviceMeta)) {
                    continue;
                }
                RequestMapping classMapping = resolver.resolve(serviceMeta);
                consumer.accept((methods) -> {
                    Method method = methods.get(0);
                    MethodDescriptor md = sd.getMethod(method.getName(), method.getParameterTypes());
                    MethodMeta methodMeta = new MethodMeta(methods, md, serviceMeta);
                    if (!resolver.accept(methodMeta)) {
                        return;
                    }
                    RequestMapping methodMapping = resolver.resolve(methodMeta);
                    if (methodMapping == null || methodMapping.getPathCondition() == null) {
                        return;
                    }
                    if (md == null) {
                        if (!(sd instanceof ReflectionServiceDescriptor)) {
                            return;
                        }
                        md = new ReflectionMethodDescriptor(method);
                        ((ReflectionServiceDescriptor) sd).addMethod(md);
                        methodMeta.setMethodDescriptor(md);
                    }
                    if (classMapping != null) {
                        methodMapping = classMapping.combine(methodMapping);
                    }
                    methodMeta.initParameters();
                    MethodMetadata methodMetadata = MethodMetadata.fromMethodDescriptor(md);
                    register0(methodMapping, new HandlerMeta(invoker, methodMeta, methodMetadata, md, sd), counter);
                });
            }
        });
        onMappingChanged();
        LOGGER.info(
                "Registered {} rest mappings for service [{}] at url [{}] in {}ms",
                counter,
                ClassUtils.toShortString(service),
                url.toString(""),
                System.currentTimeMillis() - start);
    }

    private void register0(RequestMapping mapping, HandlerMeta handler, AtomicInteger counter) {
        lock.writeLock().lock();
        try {
            Registration registration = new Registration(mapping, handler);
            for (PathExpression path : mapping.getPathCondition().getExpressions()) {
                Registration exists = tree.addPath(path, registration);
                if (exists == null) {
                    counter.incrementAndGet();
                    if (LOGGER.isDebugEnabled()) {
                        String msg = "Register rest mapping: '{}' -> mapping={}, method={}";
                        LOGGER.debug(msg, path, mapping, handler.getMethod());
                    }
                } else if (LOGGER.isWarnEnabled()) {
                    LOGGER.internalWarn(Messages.DUPLICATE_MAPPING.format(path, mapping, handler.getMethod(), exists));
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void unregister(Invoker<?> invoker) {
        if (tree == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            tree.remove(r -> r.getMeta().getInvoker() == invoker);
            onMappingChanged();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void destroy() {
        if (tree == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            tree.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public HandlerMeta lookup(HttpRequest request) {
        if (tree == null) {
            return null;
        }

        String stringPath = PathUtils.normalize(request.uri());
        request.setAttribute(RestConstants.PATH_ATTRIBUTE, stringPath);
        KeyString path = new KeyString(stringPath, restConfig.getCaseSensitiveMatchOrDefault());

        List<Candidate> candidates = new ArrayList<>();
        List<RequestMapping> partialMatches = new LinkedList<>();
        tryMatch(request, path, candidates, partialMatches);

        if (candidates.isEmpty()) {
            int end = path.length();

            if (end > 1 && restConfig.getTrailingSlashMatchOrDefault()) {
                if (path.charAt(end - 1) == '/') {
                    tryMatch(request, path.subSequence(0, --end), candidates, partialMatches);
                }
            }

            if (candidates.isEmpty()) {
                for (int i = end - 1; i >= 0; i--) {
                    char ch = path.charAt(i);
                    if (ch == '/') {
                        break;
                    }
                    if (ch == '.' && restConfig.getSuffixPatternMatchOrDefault()) {
                        if (contentNegotiator.supportExtension(path.toString(i + 1, end))) {
                            tryMatch(request, path.subSequence(0, i), candidates, partialMatches);
                            if (!candidates.isEmpty()) {
                                break;
                            }
                            end = i;
                        }
                    }
                    if (ch == '~') {
                        request.setAttribute(RestConstants.SIG_ATTRIBUTE, path.toString(i + 1, end));
                        tryMatch(request, path.subSequence(0, i), candidates, partialMatches);
                        if (!candidates.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        int size = candidates.size();
        if (size == 0) {
            handleNoMatch(request, partialMatches);
            return null;
        }
        if (size > 1) {
            candidates.sort((c1, c2) -> {
                int comparison = c1.expression.compareTo(c2.expression, stringPath);
                if (comparison != 0) {
                    return comparison;
                }
                comparison = c1.mapping.compareTo(c2.mapping, request);
                if (comparison != 0) {
                    return comparison;
                }
                return c1.variableMap.size() - c2.variableMap.size();
            });

            LOGGER.debug("Candidate rest mappings: {}", candidates);

            Candidate first = candidates.get(0);
            Candidate second = candidates.get(1);
            if (first.mapping.compareTo(second.mapping, request) == 0) {
                throw new RestMappingException(Messages.AMBIGUOUS_MAPPING, path, first, second);
            }
        }

        Candidate winner = candidates.get(0);
        RequestMapping mapping = winner.mapping;
        HandlerMeta handler = winner.meta;
        request.setAttribute(RestConstants.MAPPING_ATTRIBUTE, mapping);
        request.setAttribute(RestConstants.HANDLER_ATTRIBUTE, handler);

        LOGGER.debug("Matched rest mapping={}, method={}", mapping, handler.getMethod());

        if (!winner.variableMap.isEmpty()) {
            request.setAttribute(RestConstants.URI_TEMPLATE_VARIABLES_ATTRIBUTE, winner.variableMap);
        }

        ProducesCondition producesCondition = mapping.getProducesCondition();
        if (producesCondition != null) {
            request.setAttribute(RestConstants.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, producesCondition.getMediaTypes());
        }

        return handler;
    }

    private void tryMatch(
            HttpRequest request, KeyString path, List<Candidate> candidates, List<RequestMapping> partialMatches) {
        List<Match<Registration>> matches = new ArrayList<>();

        lock.readLock().lock();
        try {
            tree.match(path, matches);
        } finally {
            lock.readLock().unlock();
        }

        int size = matches.size();
        if (size == 0) {
            return;
        }
        for (int i = 0; i < size; i++) {
            Match<Registration> match = matches.get(i);
            RequestMapping mapping = match.getValue().getMapping().match(request, match.getExpression());
            if (mapping != null) {
                Candidate candidate = new Candidate();
                candidate.mapping = mapping;
                candidate.meta = match.getValue().getMeta();
                candidate.expression = match.getExpression();
                candidate.variableMap = match.getVariableMap();
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            for (int i = 0; i < size; i++) {
                partialMatches.add(matches.get(i).getValue().getMapping());
            }
        }
    }

    private void handleNoMatch(HttpRequest request, List<RequestMapping> partialMatches) {
        if (partialMatches.isEmpty()) {
            return;
        }
        boolean methodsMismatch = true;
        boolean consumesMismatch = true;
        boolean producesMismatch = true;
        boolean paramsMismatch = true;
        for (RequestMapping mapping : partialMatches) {
            if (methodsMismatch) {
                methodsMismatch = !mapping.matchMethod(request.method());
            }
            if (consumesMismatch) {
                consumesMismatch = !mapping.matchConsumes(request);
            }
            if (producesMismatch) {
                producesMismatch = !mapping.matchProduces(request);
            }
            if (paramsMismatch) {
                paramsMismatch = !mapping.matchParams(request);
            }
        }
        if (methodsMismatch) {
            throw new HttpStatusException(405, "Request method '" + request.method() + "' not supported");
        }
        if (consumesMismatch) {
            throw new HttpStatusException(415, "Content type '" + request.contentType() + "' not supported");
        }
        if (producesMismatch) {
            throw new HttpStatusException(406, "Could not find acceptable representation");
        }
        if (paramsMismatch) {
            throw new HttpStatusException(400, "Unsatisfied query parameter conditions");
        }
    }

    @Override
    public boolean exists(String stringPath, String method) {
        if (tree == null) {
            return false;
        }

        KeyString path = new KeyString(stringPath, restConfig.getCaseSensitiveMatchOrDefault());
        if (tryExists(path, method)) {
            return true;
        }

        int end = path.length();
        if (restConfig.getTrailingSlashMatchOrDefault()) {
            if (path.charAt(end - 1) == '/') {
                end--;
                if (tryExists(path.subSequence(0, end - 1), method)) {
                    return true;
                }
            }
        }

        for (int i = end - 1; i >= 0; i--) {
            char ch = path.charAt(i);
            if (ch == '/') {
                break;
            }
            if (ch == '.' && restConfig.getSuffixPatternMatchOrDefault()) {
                if (contentNegotiator.supportExtension(path.toString(i + 1, end))) {
                    if (tryExists(path.subSequence(0, i), method)) {
                        return true;
                    }
                    end = i;
                }
            }
            if (ch == '~') {
                return tryExists(path.subSequence(0, i), method);
            }
        }

        return false;
    }

    @Override
    public Collection<Registration> getRegistrations() {
        lock.readLock().lock();
        try {
            Map<Registration, Boolean> registrations = new IdentityHashMap<>();
            tree.walk((expr, registration) -> registrations.put(registration, Boolean.TRUE));
            return registrations.keySet();
        } finally {
            lock.readLock().unlock();
        }
    }

    private boolean tryExists(KeyString path, String method) {
        List<Match<Registration>> matches = new ArrayList<>();
        lock.readLock().lock();
        try {
            tree.match(path, matches);
        } finally {
            lock.readLock().unlock();
        }
        for (int i = 0, size = matches.size(); i < size; i++) {
            if (matches.get(i).getValue().getMapping().matchMethod(method)) {
                return true;
            }
        }
        return false;
    }

    private void onMappingChanged() {
        if (openAPIService != null) {
            openAPIService.refresh();
        }
    }

    private static final class Candidate {

        RequestMapping mapping;
        HandlerMeta meta;
        PathExpression expression;
        Map<String, String> variableMap;

        @Override
        public String toString() {
            return "Candidate{mapping=" + mapping + ", method=" + meta.getMethod() + '}';
        }
    }
}
