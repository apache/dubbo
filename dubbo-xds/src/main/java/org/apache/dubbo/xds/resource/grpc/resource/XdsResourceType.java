/*
 * Copyright 2022 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource.grpc.resource;


import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;
import org.apache.dubbo.xds.resource.grpc.resource.exception.ResourceInvalidException;
import org.apache.dubbo.xds.resource.grpc.resource.filter.FilterRegistry;
import org.apache.dubbo.xds.resource.grpc.resource.update.ResourceUpdate;

import javax.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import io.grpc.LoadBalancerRegistry;

import static com.google.common.base.Preconditions.checkNotNull;

abstract class XdsResourceType<T extends ResourceUpdate> {
  static final String TYPE_URL_RESOURCE =
      "type.googleapis.com/envoy.service.discovery.v3.Resource";
  static final String TRANSPORT_SOCKET_NAME_TLS = "envoy.transport_sockets.tls";
  @VisibleForTesting
  static final String AGGREGATE_CLUSTER_TYPE_NAME = "envoy.clusters.aggregate";
  @VisibleForTesting
  static final String HASH_POLICY_FILTER_STATE_KEY = "io.grpc.channel_id";
  @VisibleForTesting
  static boolean enableRouteLookup = getFlag("GRPC_EXPERIMENTAL_XDS_RLS_LB", true);
  @VisibleForTesting
  static boolean enableLeastRequest =
      !Strings.isNullOrEmpty(System.getenv("GRPC_EXPERIMENTAL_ENABLE_LEAST_REQUEST"))
          ? Boolean.parseBoolean(System.getenv("GRPC_EXPERIMENTAL_ENABLE_LEAST_REQUEST"))
          : Boolean.parseBoolean(System.getProperty("io.grpc.xds.experimentalEnableLeastRequest"));

  @VisibleForTesting
  static boolean enableWrr = getFlag("GRPC_EXPERIMENTAL_XDS_WRR_LB", true);

  @VisibleForTesting
  static boolean enablePickFirst = getFlag("GRPC_EXPERIMENTAL_PICKFIRST_LB_CONFIG", true);

  static final String TYPE_URL_CLUSTER_CONFIG =
      "type.googleapis.com/envoy.extensions.clusters.aggregate.v3.ClusterConfig";
  static final String TYPE_URL_TYPED_STRUCT_UDPA =
      "type.googleapis.com/udpa.type.v1.TypedStruct";
  static final String TYPE_URL_TYPED_STRUCT =
      "type.googleapis.com/xds.type.v3.TypedStruct";

  @Nullable
  abstract String extractResourceName(Message unpackedResource);

  abstract Class<? extends Message> unpackedClassName();

  abstract String typeName();

  abstract String typeUrl();

  // Do not confuse with the SotW approach: it is the mechanism in which the client must specify all
  // resource names it is interested in with each request. Different resource types may behave
  // differently in this approach. For LDS and CDS resources, the server must return all resources
  // that the client has subscribed to in each request. For RDS and EDS, the server may only return
  // the resources that need an update.

    /**
     * 不要与 SotW 方法混淆：它是一种机制，在这种机制中，客户端必须在每个请求中指定它感兴趣的所有资源名称。在此方法中，不同的资源类型可能具有不同的行为。
     * 对于 LDS 和 CDS 资源，服务器必须返回客户端在每个请求中订阅的所有资源。对于 RDS 和 EDS，服务器可能只返回需要更新的资源。
     * @return
     */
  abstract boolean isFullStateOfTheWorld();

  static class Args {
    final ServerInfo serverInfo;
    final String versionInfo;
    final String nonce;
    final Bootstrapper.BootstrapInfo bootstrapInfo;
    final FilterRegistry filterRegistry;
    final LoadBalancerRegistry loadBalancerRegistry;
//    final TlsContextManager tlsContextManager;
    // Management server is required to always send newly requested resources, even if they
    // may have been sent previously (proactively). Thus, client does not need to cache
    // unrequested resources.
    // Only resources in the set needs to be parsed. Null means parse everything.
    final @Nullable Set<String> subscribedResources;

    public Args(ServerInfo serverInfo, String versionInfo, String nonce,
                Bootstrapper.BootstrapInfo bootstrapInfo,
                FilterRegistry filterRegistry,
                LoadBalancerRegistry loadBalancerRegistry,
//                TlsContextManager tlsContextManager,
                @Nullable Set<String> subscribedResources) {
      this.serverInfo = serverInfo;
      this.versionInfo = versionInfo;
      this.nonce = nonce;
      this.bootstrapInfo = bootstrapInfo;
      this.filterRegistry = filterRegistry;
      this.loadBalancerRegistry = loadBalancerRegistry;
//      this.tlsContextManager = tlsContextManager;
      this.subscribedResources = subscribedResources;
    }
  }

  ValidatedResourceUpdate<T> parse(Args args, List<Any> resources) {
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
        errors.add(String.format("%s response Resource index %d - can't decode %s: %s",
                typeName(), i, unpackedClassName().getSimpleName(), e.getMessage()));
        continue;
      }
      String name = extractResourceName(unpackedMessage);
      if (name == null || !isResourceNameValid(name, resource.getTypeUrl())) {
        errors.add(
            "Unsupported resource name: " + name + " for type: " + typeName());
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
        errors.add(String.format("%s response %s '%s' validation error: %s",
                typeName(), unpackedClassName().getSimpleName(), cname, e.getMessage()));
        invalidResources.add(cname);
        continue;
      }

      // Resource parsed successfully.
      parsedResources.put(cname, new ParsedResource<T>(resourceUpdate, resource));
    }
    return new ValidatedResourceUpdate<T>(parsedResources, unpackedResources, invalidResources,
        errors);

  }

    static String canonifyResourceName(String resourceName) {
        checkNotNull(resourceName, "resourceName");
        if (!resourceName.startsWith("xdstp:")) {
            return resourceName;
        }
        URI uri = URI.create(resourceName);
        String rawQuery = uri.getRawQuery();
        Splitter ampSplitter = Splitter.on('&').omitEmptyStrings();
        if (rawQuery == null) {
            return resourceName;
        }
        List<String> queries = ampSplitter.splitToList(rawQuery);
        if (queries.size() < 2) {
            return resourceName;
        }
        List<String> canonicalContextParams = new ArrayList<>(queries.size());
        for (String query : queries) {
            canonicalContextParams.add(query);
        }
        Collections.sort(canonicalContextParams);
        String canonifiedQuery = Joiner.on('&').join(canonicalContextParams);
        return resourceName.replace(rawQuery, canonifiedQuery);
    }


    static boolean isResourceNameValid(String resourceName, String typeUrl) {
        checkNotNull(resourceName, "resourceName");
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
        Splitter slashSplitter = Splitter.on('/').omitEmptyStrings();
        if (path == null) {
            return false;
        }
        List<String> pathSegs = slashSplitter.splitToList(path);
        if (pathSegs.size() < 2) {
            return false;
        }
        String type = pathSegs.get(0);
        if (!type.equals(slashSplitter.splitToList(typeUrl).get(1))) {
            return false;
        }
        return true;
    }

  abstract T doParse(Args args, Message unpackedMessage) throws ResourceInvalidException;

  /**
   * Helper method to unpack serialized {@link Any} message, while replacing
   * Type URL {@code compatibleTypeUrl} with {@code typeUrl}.
   *
   * @param <T> The type of unpacked message
   * @param any serialized message to unpack
   * @param clazz the class to unpack the message to
   * @param typeUrl type URL to replace message Type URL, when it's compatible
   * @param compatibleTypeUrl compatible Type URL to be replaced with {@code typeUrl}
   * @return Unpacked message
   * @throws InvalidProtocolBufferException if the message couldn't be unpacked
   */
  static <T extends Message> T unpackCompatibleType(
      Any any, Class<T> clazz, String typeUrl, String compatibleTypeUrl)
      throws InvalidProtocolBufferException {
    if (any.getTypeUrl().equals(compatibleTypeUrl)) {
      any = any.toBuilder().setTypeUrl(typeUrl).build();
    }
    return any.unpack(clazz);
  }

  private Any maybeUnwrapResources(Any resource)
      throws InvalidProtocolBufferException {
    if (resource.getTypeUrl().equals(TYPE_URL_RESOURCE)) {
      return unpackCompatibleType(resource, Resource.class, TYPE_URL_RESOURCE,
          null).getResource();
    } else {
      return resource;
    }
  }

  static final class ParsedResource<T extends ResourceUpdate> {
    private final T resourceUpdate;
    private final Any rawResource;

    public ParsedResource(T resourceUpdate, Any rawResource) {
      this.resourceUpdate = checkNotNull(resourceUpdate, "resourceUpdate");
      this.rawResource = checkNotNull(rawResource, "rawResource");
    }

    T getResourceUpdate() {
      return resourceUpdate;
    }

    Any getRawResource() {
      return rawResource;
    }
  }

  static final class ValidatedResourceUpdate<T extends ResourceUpdate> {
    Map<String, ParsedResource<T>> parsedResources;
    Set<String> unpackedResources;
    Set<String> invalidResources;
    List<String> errors;

    // validated resource update
    public ValidatedResourceUpdate(Map<String, ParsedResource<T>> parsedResources,
                                   Set<String> unpackedResources,
                                   Set<String> invalidResources,
                                   List<String> errors) {
      this.parsedResources = parsedResources;
      this.unpackedResources = unpackedResources;
      this.invalidResources = invalidResources;
      this.errors = errors;
    }
  }

  private static boolean getFlag(String envVarName, boolean enableByDefault) {
    String envVar = System.getenv(envVarName);
    if (enableByDefault) {
      return Strings.isNullOrEmpty(envVar) || Boolean.parseBoolean(envVar);
    } else {
      return !Strings.isNullOrEmpty(envVar) && Boolean.parseBoolean(envVar);
    }
  }

  @VisibleForTesting
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
      this.struct = checkNotNull(struct, "struct");
      this.errorDetail = null;
    }

    private StructOrError(String errorDetail) {
      this.struct = null;
      this.errorDetail = checkNotNull(errorDetail, "errorDetail");
    }

    /**
     * Returns struct if exists, otherwise null.
     */
    @VisibleForTesting
    @Nullable
    T getStruct() {
      return struct;
    }

    /**
     * Returns error detail if exists, otherwise null.
     */
    @VisibleForTesting
    @Nullable
    String getErrorDetail() {
      return errorDetail;
    }
  }
}
