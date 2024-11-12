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
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.LRUCache;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.Registration;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingRegistry;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultOpenAPIService implements OpenAPIService {

    private static final FluentLogger LOG = FluentLogger.of(DefaultOpenAPIService.class);

    private final LRUCache<String, SoftReference<String>> cache = new LRUCache<>(64);
    private final FrameworkModel frameworkModel;
    private final ExtensionFactory extensionFactory;
    private final DefinitionResolver definitionResolver;
    private final DefinitionMerger definitionMerger;
    private final DefinitionFilter definitionFilter;
    private final DefinitionEncoder definitionEncoder;
    private RequestMappingRegistry requestMappingRegistry;

    private volatile List<OpenAPI> openAPIs;
    private boolean exported;
    private ScheduledFuture<?> exportFuture;

    public DefaultOpenAPIService(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        extensionFactory = frameworkModel.getOrRegisterBean(ExtensionFactory.class);
        definitionResolver = new DefinitionResolver(frameworkModel);
        definitionMerger = new DefinitionMerger(frameworkModel);
        definitionFilter = new DefinitionFilter(frameworkModel);
        definitionEncoder = new DefinitionEncoder(frameworkModel);
    }

    public void setRequestMappingRegistry(RequestMappingRegistry requestMappingRegistry) {
        this.requestMappingRegistry = requestMappingRegistry;
    }

    @Override
    public OpenAPI getOpenAPI(OpenAPIRequest request) {
        if (openAPIs == null) {
            synchronized (this) {
                if (openAPIs == null) {
                    openAPIs = resolveOpenAPIs();
                }
            }
        }
        return definitionFilter.filter(definitionMerger.merge(openAPIs, request), request);
    }

    private List<OpenAPI> resolveOpenAPIs() {
        Map<Key, Map<Method, List<Registration>>> byClassMap = new HashMap<>();
        for (Registration registration : requestMappingRegistry.getRegistrations()) {
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
        String cacheKey = request.toString();
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
        OpenAPIRequest request = new OpenAPIRequest();
        request.setPretty(true);
        String openAPI = getDocument(request);
        LOG.info("Refreshed OpenAPI documents: {}", openAPI);
    }

    @Override
    public void export() {
        if (extensionFactory.getExtensions(DocumentPublisher.class).length == 0) {
            return;
        }

        if (exportFuture != null) {
            exportFuture.cancel(false);
        }
        exportFuture = frameworkModel
                .getBean(FrameworkExecutorRepository.class)
                .getMetadataRetryExecutor()
                .schedule(this::doExport, 30, TimeUnit.SECONDS);
        exported = true;
    }

    private void doExport() {
        for (DocumentPublisher publisher : extensionFactory.getExtensions(DocumentPublisher.class)) {
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
