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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.common.logger.FluentLogger;
import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.LRUCache;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.exception.HttpResultPayloadException;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.rest.OpenAPIRequest;
import org.apache.dubbo.remoting.http12.rest.OpenAPIService;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RadixTree;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RadixTree.Match;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.Registration;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingRegistry;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.util.PathUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultOpenAPIService implements OpenAPIRequestHandler, OpenAPIService {

    private static final FluentLogger LOG = FluentLogger.of(DefaultOpenAPIService.class);
    private static final String API_DOCS = "/api-docs";

    private final LRUCache<String, SoftReference<String>> cache = new LRUCache<>(64);
    private final FrameworkModel frameworkModel;
    private final ConfigFactory configFactory;
    private final ExtensionFactory extensionFactory;
    private final DefinitionResolver definitionResolver;
    private final DefinitionMerger definitionMerger;
    private final DefinitionFilter definitionFilter;
    private final DefinitionEncoder definitionEncoder;
    private final RadixTree<OpenAPIRequestHandler> tree;

    private volatile List<OpenAPI> openAPIs;
    private boolean exported;
    private ScheduledFuture<?> exportFuture;

    public DefaultOpenAPIService(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        configFactory = frameworkModel.getOrRegisterBean(ConfigFactory.class);
        extensionFactory = frameworkModel.getOrRegisterBean(ExtensionFactory.class);
        definitionResolver = new DefinitionResolver(frameworkModel);
        definitionMerger = new DefinitionMerger(frameworkModel);
        definitionFilter = new DefinitionFilter(frameworkModel);
        definitionEncoder = new DefinitionEncoder(frameworkModel);
        tree = initRequestHandlers();
    }

    private RadixTree<OpenAPIRequestHandler> initRequestHandlers() {
        RadixTree<OpenAPIRequestHandler> tree = new RadixTree<>(false);
        for (OpenAPIRequestHandler handler : extensionFactory.getExtensions(OpenAPIRequestHandler.class)) {
            for (String path : handler.getPaths()) {
                tree.addPath(path, handler);
            }
        }
        tree.addPath(this, API_DOCS, API_DOCS + "/{group}");
        return tree;
    }

    @Override
    public HttpResult<?> handle(String path, HttpRequest httpRequest, HttpResponse httpResponse) {
        OpenAPIRequest request = httpRequest.attribute(OpenAPIRequest.class.getName());
        String group = RequestUtils.getPathVariable(httpRequest, "group");
        if (group != null) {
            request.setGroup(StringUtils.substringBeforeLast(group, '.'));
        }
        return HttpResult.builder()
                .contentType(MediaType.APPLICATION + '/' + request.getFormat())
                .body(handleDocument(request, httpRequest).getBytes(StandardCharsets.UTF_8))
                .build();
    }

    @Override
    public Collection<String> getOpenAPIGroups() {
        Set<String> groups = new LinkedHashSet<>();
        groups.add(Constants.DEFAULT_GROUP);
        for (OpenAPI openAPI : getOpenAPIs()) {
            groups.add(openAPI.getGroup());
            openAPI.walkOperations(operation -> {
                String group = operation.getGroup();
                if (StringUtils.isNotEmpty(group)) {
                    groups.add(group);
                }
            });
        }
        return groups;
    }

    public OpenAPI getOpenAPI(OpenAPIRequest request) {
        return definitionFilter.filter(definitionMerger.merge(getOpenAPIs(), request), request);
    }

    private List<OpenAPI> getOpenAPIs() {
        if (openAPIs == null) {
            synchronized (this) {
                if (openAPIs == null) {
                    openAPIs = resolveOpenAPIs();
                }
            }
        }
        return openAPIs;
    }

    private List<OpenAPI> resolveOpenAPIs() {
        RequestMappingRegistry registry = frameworkModel.getBean(RequestMappingRegistry.class);
        if (registry == null) {
            return Collections.emptyList();
        }

        Map<Key, Map<Method, List<Registration>>> byClassMap = new HashMap<>();
        for (Registration registration : registry.getRegistrations()) {
            HandlerMeta meta = registration.getMeta();
            byClassMap
                    .computeIfAbsent(new Key(meta.getService()), k -> new IdentityHashMap<>())
                    .computeIfAbsent(meta.getMethod().getMethod(), k -> new ArrayList<>(1))
                    .add(registration);
        }

        List<OpenAPI> openAPIs = new ArrayList<>(byClassMap.size());
        for (Map.Entry<Key, Map<Method, List<Registration>>> entry : byClassMap.entrySet()) {
            OpenAPI openAPI = definitionResolver.resolve(
                    entry.getKey().serviceMeta, entry.getValue().values());
            if (openAPI != null) {
                openAPIs.add(openAPI);
            }
        }
        openAPIs.sort(Comparator.comparingInt(OpenAPI::getPriority));

        return openAPIs;
    }

    @Override
    public String getDocument(OpenAPIRequest request) {
        String path = null;
        try {
            request = Helper.formatRequest(request);

            HttpRequest httpRequest = RpcContext.getServiceContext().getRequest(HttpRequest.class);
            if (!RequestUtils.isRestRequest(httpRequest)) {
                return handleDocument(request, null);
            }

            path = RequestUtils.getPathVariable(httpRequest, "path");
            if (StringUtils.isEmpty(path)) {
                String url = PathUtils.join(httpRequest.path(), "swagger-ui/index.html");
                throw HttpResult.found(url).toPayload();
            }

            path = '/' + path;
            List<Match<OpenAPIRequestHandler>> matches = tree.matchRelaxed(path);
            if (matches.isEmpty()) {
                throw HttpResult.notFound().toPayload();
            }

            Collections.sort(matches);
            Match<OpenAPIRequestHandler> match = matches.get(0);
            HttpResponse httpResponse = RpcContext.getServiceContext().getResponse(HttpResponse.class);
            if (request.getFormat() == null) {
                request.setFormat(Helper.parseFormat(httpResponse.contentType()));
            }
            httpRequest.setAttribute(OpenAPIRequest.class.getName(), request);
            httpRequest.setAttribute(RestConstants.URI_TEMPLATE_VARIABLES_ATTRIBUTE, match.getVariableMap());
            throw match.getValue().handle(path, httpRequest, httpResponse).toPayload();
        } catch (HttpResultPayloadException e) {
            throw e;
        } catch (Throwable t) {
            Level level = ExceptionUtils.resolveLogLevel(ExceptionUtils.unwrap(t));
            LOG.log(level, "Failed to processing OpenAPI request {} for path: '{}'", request, path, t);
            throw t;
        }
    }

    private String handleDocument(OpenAPIRequest request, HttpRequest httpRequest) {
        if (Boolean.FALSE.equals(configFactory.getGlobalConfig().getCache())) {
            return definitionEncoder.encode(getOpenAPI(request), request);
        }

        StringBuilder sb = new StringBuilder();
        if (httpRequest != null) {
            String host = httpRequest.serverHost();
            if (host != null) {
                String referer = httpRequest.header(Constants.REFERER);
                sb.append(referer != null && referer.contains(host) ? '/' : host);
            }
        }
        sb.append('|').append(request.toString());

        String cacheKey = sb.toString();
        SoftReference<String> ref = cache.get(cacheKey);
        if (ref != null) {
            String value = ref.get();
            if (value != null) {
                return value;
            }
        }
        String value = definitionEncoder.encode(getOpenAPI(request), request);
        cache.put(cacheKey, new SoftReference<>(value));
        return value;
    }

    @Override
    public void refresh() {
        LOG.debug("Refreshing OpenAPI documents");
        openAPIs = null;
        cache.clear();
        if (exported) {
            export();
        }
    }

    @Override
    public void export() {
        if (!extensionFactory.hasExtensions(OpenAPIDocumentPublisher.class)) {
            return;
        }

        try {
            if (exportFuture != null) {
                exportFuture.cancel(false);
            }
            exportFuture = frameworkModel
                    .getBean(FrameworkExecutorRepository.class)
                    .getMetadataRetryExecutor()
                    .schedule(this::doExport, 30, TimeUnit.SECONDS);
            exported = true;
        } catch (Throwable t) {
            LOG.internalWarn("Failed to export OpenAPI documents", t);
        }
    }

    private void doExport() {
        for (OpenAPIDocumentPublisher publisher : extensionFactory.getExtensions(OpenAPIDocumentPublisher.class)) {
            try {
                publisher.publish(request -> {
                    OpenAPI openAPI = getOpenAPI(request);
                    String document = definitionEncoder.encode(openAPI, request);
                    return Pair.of(openAPI, document);
                });
            } catch (Throwable t) {
                LOG.internalWarn("Failed to publish OpenAPI document by {}", publisher, t);
            }
        }
        exportFuture = null;
    }

    private static final class Key {

        private final ServiceMeta serviceMeta;

        public Key(ServiceMeta serviceMeta) {
            this.serviceMeta = serviceMeta;
        }

        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass", "EqualsDoesntCheckParameterClass"})
        @Override
        public boolean equals(Object obj) {
            return serviceMeta.getType() == ((Key) obj).serviceMeta.getType();
        }

        @Override
        public int hashCode() {
            return serviceMeta.getType().hashCode();
        }
    }
}
