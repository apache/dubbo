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
package org.apache.dubbo.xds.resource;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;
import org.apache.dubbo.xds.resource.exception.ResourceInvalidException;
import org.apache.dubbo.xds.resource.filter.FilterRegistry;
import org.apache.dubbo.xds.resource.listener.security.TlsContextManager;
import org.apache.dubbo.xds.resource.update.ParsedResource;
import org.apache.dubbo.xds.resource.update.ResourceUpdate;
import org.apache.dubbo.xds.resource.update.ValidatedResourceUpdate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import io.grpc.LoadBalancerRegistry;

public abstract class XdsResourceType<T extends ResourceUpdate> {
    static final String TYPE_URL_RESOURCE = "type.googleapis.com/envoy.service.discovery.v3.Resource";
    static final String TRANSPORT_SOCKET_NAME_TLS = "envoy.transport_sockets.tls";
    static final String AGGREGATE_CLUSTER_TYPE_NAME = "envoy.clusters.aggregate";
    static final String HASH_POLICY_FILTER_STATE_KEY = "io.grpc.channel_id";
    static boolean enableRouteLookup = getFlag("GRPC_EXPERIMENTAL_XDS_RLS_LB", true);
    static boolean enableLeastRequest = !StringUtils.isBlank(System.getenv("GRPC_EXPERIMENTAL_ENABLE_LEAST_REQUEST"))
            ? Boolean.parseBoolean(System.getenv("GRPC_EXPERIMENTAL_ENABLE_LEAST_REQUEST"))
            : Boolean.parseBoolean(System.getProperty("io.grpc.xds.experimentalEnableLeastRequest"));

    static boolean enableWrr = getFlag("GRPC_EXPERIMENTAL_XDS_WRR_LB", true);

    static boolean enablePickFirst = getFlag("GRPC_EXPERIMENTAL_PICKFIRST_LB_CONFIG", true);

    static final String TYPE_URL_CLUSTER_CONFIG =
            "type.googleapis.com/envoy.extensions.clusters.aggregate.v3" + ".ClusterConfig";
    static final String TYPE_URL_TYPED_STRUCT_UDPA = "type.googleapis.com/udpa.type.v1.TypedStruct";
    static final String TYPE_URL_TYPED_STRUCT = "type.googleapis.com/xds.type.v3.TypedStruct";

    @Nullable
    abstract String extractResourceName(Message unpackedResource);

    abstract Class<? extends Message> unpackedClassName();

    abstract String typeName();

    public abstract String typeUrl();

    // Do not confuse with the SotW approach: it is the mechanism in which the client must specify all
    // resource names it is interested in with each request. Different resource types may behave
    // differently in this approach. For LDS and CDS resources, the server must return all resources
    // that the client has subscribed to in each request. For RDS and EDS, the server may only return
    // the resources that need an update.
    abstract boolean isFullStateOfTheWorld();

    public static final Args xdsResourceTypeArgs =
            new Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null); // TODO

    public static class Args {
        final ServerInfo serverInfo;
        final String versionInfo;
        final String nonce;
        final Bootstrapper.BootstrapInfo bootstrapInfo;
        final FilterRegistry filterRegistry;
        final LoadBalancerRegistry loadBalancerRegistry;
        final TlsContextManager tlsContextManager;
        // Management server is required to always send newly requested resources, even if they
        // may have been sent previously (proactively). Thus, client does not need to cache
        // unrequested resources.
        // Only resources in the set needs to be parsed. Null means parse everything.
        final @Nullable Set<String> subscribedResources;

        public Args(
                ServerInfo serverInfo,
                String versionInfo,
                String nonce,
                Bootstrapper.BootstrapInfo bootstrapInfo,
                FilterRegistry filterRegistry,
                LoadBalancerRegistry loadBalancerRegistry,
                TlsContextManager tlsContextManager,
                @Nullable Set<String> subscribedResources) {
            this.serverInfo = serverInfo;
            this.versionInfo = versionInfo;
            this.nonce = nonce;
            this.bootstrapInfo = bootstrapInfo;
            this.filterRegistry = filterRegistry;
            this.loadBalancerRegistry = loadBalancerRegistry;
            this.tlsContextManager = tlsContextManager;
            this.subscribedResources = subscribedResources;
        }
    }

    public ValidatedResourceUpdate<T> parse(Args args, List<Any> resources) {
        Map<String, ParsedResource<T>> parsedResources = new HashMap<>(resources.size());
        Set<String> unpackedResources = new HashSet<>(resources.size());
        Set<String> invalidResources = new HashSet<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < resources.size(); i++) {
            Any resource = resources.get(i);

            Message unpackedMessage;
            try {
                resource = maybeUnwrapResources(resource);
                unpackedMessage = unpackCompatibleType(resource, unpackedClassName(), typeUrl(), null);
            } catch (InvalidProtocolBufferException e) {
                errors.add(String.format(
                        "%s response Resource index %d - can't decode %s: %s",
                        typeName(), i, unpackedClassName().getSimpleName(), e.getMessage()));
                continue;
            }
            String name = extractResourceName(unpackedMessage);
            if (name == null || !isResourceNameValid(name, resource.getTypeUrl())) {
                errors.add("Unsupported resource name: " + name + " for type: " + typeName());
                continue;
            }
            String cname = canonifyResourceName(name);
            if (args.subscribedResources != null && !args.subscribedResources.contains(name)) {
                continue;
            }
            unpackedResources.add(cname);

            T resourceUpdate;
            try {
                resourceUpdate = doParse(args, unpackedMessage);
            } catch (ResourceInvalidException e) {
                errors.add(String.format(
                        "%s response %s '%s' validation error: %s",
                        typeName(), unpackedClassName().getSimpleName(), cname, e.getMessage()));
                invalidResources.add(cname);
                continue;
            }

            // Resource parsed successfully.
            parsedResources.put(cname, new ParsedResource<T>(resourceUpdate, resource));
        }
        return new ValidatedResourceUpdate<T>(parsedResources, unpackedResources, invalidResources, errors);
    }

    static String canonifyResourceName(String resourceName) {
        if (resourceName == null) {
            throw new NullPointerException("resourceName must not be null");
        }
        if (!resourceName.startsWith("xdstp:")) {
            return resourceName;
        }
        URI uri = URI.create(resourceName);
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null) {
            return resourceName;
        }
        List<String> queries = Arrays.stream(rawQuery.split("&"))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        if (queries.size() < 2) {
            return resourceName;
        }
        List<String> canonicalContextParams = new ArrayList<>(queries.size());
        for (String query : queries) {
            canonicalContextParams.add(query);
        }
        Collections.sort(canonicalContextParams);
        String canonifiedQuery = String.join("&", canonicalContextParams);
        return resourceName.replace(rawQuery, canonifiedQuery);
    }

    static boolean isResourceNameValid(String resourceName, String typeUrl) {
        Assert.notNull(resourceName, "resourceName must not be null");
        if (!resourceName.startsWith("xdstp:")) {
            return true;
        }
        URI uri;
        try {
            uri = new URI(resourceName);
        } catch (URISyntaxException e) {
            return false;
        }
        String path = uri.getPath();
        // path must be in the form of /{resource type}/{id/*}
        if (path == null) {
            return false;
        }
        List<String> pathSegs =
                Arrays.stream(path.split("/")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        if (pathSegs.size() < 2) {
            return false;
        }
        String type = pathSegs.get(0);
        if (!type.equals(Arrays.stream(typeUrl.split("/"))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList())
                .get(1))) {
            return false;
        }
        return true;
    }

    abstract T doParse(Args args, Message unpackedMessage) throws ResourceInvalidException;

    /**
     * Helper method to unpack serialized {@link Any} message, while replacing Type URL {@code compatibleTypeUrl} with
     * {@code typeUrl}.
     *
     * @param <T>               The type of unpacked message
     * @param any               serialized message to unpack
     * @param clazz             the class to unpack the message to
     * @param typeUrl           type URL to replace message Type URL, when it's compatible
     * @param compatibleTypeUrl compatible Type URL to be replaced with {@code typeUrl}
     * @return Unpacked message
     * @throws InvalidProtocolBufferException if the message couldn't be unpacked
     */
    static <T extends Message> T unpackCompatibleType(Any any, Class<T> clazz, String typeUrl, String compatibleTypeUrl)
            throws InvalidProtocolBufferException {
        if (any.getTypeUrl().equals(compatibleTypeUrl)) {
            any = any.toBuilder().setTypeUrl(typeUrl).build();
        }
        return any.unpack(clazz);
    }

    private Any maybeUnwrapResources(Any resource) throws InvalidProtocolBufferException {
        if (resource.getTypeUrl().equals(TYPE_URL_RESOURCE)) {
            return unpackCompatibleType(resource, Resource.class, TYPE_URL_RESOURCE, null)
                    .getResource();
        } else {
            return resource;
        }
    }

    private static boolean getFlag(String envVarName, boolean enableByDefault) {
        String envVar = System.getenv(envVarName);
        if (enableByDefault) {
            return StringUtils.isEmpty(envVar) || Boolean.parseBoolean(envVar);
        } else {
            return !StringUtils.isEmpty(envVar) && Boolean.parseBoolean(envVar);
        }
    }

    static final class StructOrError<T> {

        /**
         * Returns a {@link StructOrError} for the successfully converted data object.
         */
        static <T> StructOrError<T> fromStruct(T struct) {
            return new StructOrError<>(struct);
        }

        /**
         * Returns a {@link StructOrError} for the failure to convert the data object.
         */
        static <T> StructOrError<T> fromError(String errorDetail) {
            return new StructOrError<>(errorDetail);
        }

        private final String errorDetail;
        private final T struct;

        private StructOrError(T struct) {
            Assert.notNull(struct, "struct must not be null");
            this.struct = struct;
            this.errorDetail = null;
        }

        private StructOrError(String errorDetail) {
            this.struct = null;
            Assert.notNull(errorDetail, "errorDetail must not be null");
            this.errorDetail = errorDetail;
        }

        /**
         * Returns struct if exists, otherwise null.
         */
        @Nullable
        T getStruct() {
            return struct;
        }

        /**
         * Returns error detail if exists, otherwise null.
         */
        @Nullable
        String getErrorDetail() {
            return errorDetail;
        }
    }
}
