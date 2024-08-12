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
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.nested.RestConfig;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ReflectionMethodDescriptor;
import org.apache.dubbo.rpc.model.ReflectionServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.DescriptorUtils;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.dubbo.common.logger.LoggerFactory.getErrorTypeAwareLogger;
import static org.apache.dubbo.rpc.protocol.tri.rest.Messages.AMBIGUOUS_MAPPING;

public final class DefaultRequestMappingRegistry implements RequestMappingRegistry {

    private static final ErrorTypeAwareLogger LOGGER = getErrorTypeAwareLogger(DefaultRequestMappingRegistry.class);

    private final FrameworkModel frameworkModel;
    private final ContentNegotiator contentNegotiator;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean initialized = new AtomicBoolean();

    private RestConfig restConfig;
    private List<RequestMappingResolver> resolvers;
    private RadixTree<Registration> tree;

    public DefaultRequestMappingRegistry(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        contentNegotiator = frameworkModel.getBeanFactory().getOrRegisterBean(ContentNegotiator.class);
    }

    private void init() {
        Configuration conf = ConfigurationUtils.getGlobalConfiguration(frameworkModel.defaultApplication());
        restConfig = new RestConfig();
        restConfig.setSuffixPatternMatch(conf.getBoolean(RestConstants.SUFFIX_PATTERN_MATCH_KEY, true));
        restConfig.setTrailingSlashMatch(conf.getBoolean(RestConstants.TRAILING_SLASH_MATCH_KEY, true));
        restConfig.setCaseSensitiveMatch(conf.getBoolean(RestConstants.CASE_SENSITIVE_MATCH_KEY, true));

        resolvers = frameworkModel.getActivateExtensions(RequestMappingResolver.class);
        tree = new RadixTree<>(restConfig.getCaseSensitiveMatch());
    }

    @Override
    public void register(Invoker<?> invoker) {
        if (tree == null) {
            lock.writeLock().lock();
            try {
                if (initialized.compareAndSet(false, true)) {
                    init();
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
                    if (methodMapping == null) {
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
        LOGGER.info(
                "Registered {} REST mappings for service [{}] at url [{}] in {}ms",
                counter,
                ClassUtils.toShortString(service),
                url.toString(""),
                System.currentTimeMillis() - start);
    }

    private void register0(RequestMapping mapping, HandlerMeta handler, AtomicInteger counter) {
        lock.writeLock().lock();
        try {
            Registration registration = new Registration();
            registration.mapping = mapping;
            registration.meta = handler;
            for (PathExpression path : mapping.getPathCondition().getExpressions()) {
                Registration exists = tree.addPath(path, registration);
                if (exists == null) {
                    if (LOGGER.isDebugEnabled()) {
                        String msg = "Register rest mapping: '{}' -> mapping={}, method={}";
                        LOGGER.debug(msg, path, mapping, handler.getMethod());
                    }
                    counter.incrementAndGet();
                } else if (LOGGER.isWarnEnabled()) {
                    String msg = Messages.DUPLICATE_MAPPING.format(path, mapping, handler.getMethod(), exists);
                    LOGGER.warn(LoggerCodeConstants.INTERNAL_ERROR, "", "", msg);
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
            tree.remove(mapping -> mapping.meta.getInvoker() == invoker);
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
        String path = PathUtils.normalize(request.rawPath());
        request.setAttribute(RestConstants.PATH_ATTRIBUTE, path);

        List<Candidate> candidates = new ArrayList<>();
        boolean cs = restConfig.getCaseSensitiveMatch();
        tryMatch(request, new KeyString(path, cs), candidates);

        if (candidates.isEmpty()) {
            int end = path.length();

            if (restConfig.getTrailingSlashMatch()) {
                if (path.charAt(end - 1) == '/') {
                    end--;
                    tryMatch(request, new KeyString(path, end, cs), candidates);
                }
            }

            if (candidates.isEmpty()) {
                for (int i = end - 1; i >= 0; i--) {
                    char ch = path.charAt(i);
                    if (ch == '/') {
                        break;
                    }
                    if (ch == '.' && restConfig.getSuffixPatternMatch()) {
                        if (contentNegotiator.supportExtension(path.substring(i + 1, end))) {
                            tryMatch(request, new KeyString(path, i, cs), candidates);
                            if (!candidates.isEmpty()) {
                                break;
                            }
                            end = i;
                        }
                    }
                    if (ch == '~') {
                        request.setAttribute(RestConstants.SIG_ATTRIBUTE, path.substring(i + 1, end));
                        tryMatch(request, new KeyString(path, i, cs), candidates);
                        if (!candidates.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        int size = candidates.size();
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

            LOGGER.debug("Candidate rest mappings: {}", candidates);

            Candidate first = candidates.get(0);
            Candidate second = candidates.get(1);
            if (first.mapping.compareTo(second.mapping, request) == 0) {
                throw new RestMappingException(AMBIGUOUS_MAPPING, path, first, second);
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

    private void tryMatch(HttpRequest request, KeyString path, List<Candidate> candidates) {
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
    }

    @Override
    public boolean exists(String path, String method) {
        boolean cs = restConfig.getCaseSensitiveMatch();
        if (tryExists(new KeyString(path, cs), method)) {
            return true;
        }

        int end = path.length();
        if (restConfig.getTrailingSlashMatch()) {
            if (path.charAt(end - 1) == '/') {
                end--;
                if (tryExists(new KeyString(path, end, cs), method)) {
                    return true;
                }
            }
        }

        for (int i = end - 1; i >= 0; i--) {
            char ch = path.charAt(i);
            if (ch == '/') {
                break;
            }
            if (ch == '.' && restConfig.getSuffixPatternMatch()) {
                if (contentNegotiator.supportExtension(path.substring(i + 1, end))) {
                    if (tryExists(new KeyString(path, i, cs), method)) {
                        return true;
                    }
                    end = i;
                }
            }
            if (ch == '~') {
                return tryExists(new KeyString(path, i, cs), method);
            }
        }

        return false;
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

        @Override
        public String toString() {
            return "Registration{mapping=" + mapping + ", method=" + meta.getMethod() + '}';
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
