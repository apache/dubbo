/*
 * Copyright 2020 The gRPC Authors
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

package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.Bootstrapper.AuthorityInfo;
import org.apache.dubbo.xds.resource.grpc.Bootstrapper.ServerInfo;
import org.apache.dubbo.xds.resource.grpc.LoadStatsManager2.ClusterDropStats;
import org.apache.dubbo.xds.resource.grpc.LoadStatsManager2.ClusterLocalityStats;
import org.apache.dubbo.xds.resource.grpc.XdsClient.ResourceStore;
import org.apache.dubbo.xds.resource.grpc.XdsClient.TimerLaunch;
import org.apache.dubbo.xds.resource.grpc.XdsClient.XdsResponseHandler;
import org.apache.dubbo.xds.resource.grpc.XdsResourceType.ParsedResource;
import org.apache.dubbo.xds.resource.grpc.XdsResourceType.ValidatedResourceUpdate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Any;
import io.grpc.ChannelCredentials;
import io.grpc.Context;
import io.grpc.Grpc;
import io.grpc.InternalLogId;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.SynchronizationContext;
import io.grpc.SynchronizationContext.ScheduledHandle;
import io.grpc.internal.BackoffPolicy;
import io.grpc.internal.TimeProvider;

import javax.annotation.Nullable;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.dubbo.xds.resource.grpc.Bootstrapper.XDSTP_SCHEME;

/**
 * XdsClient implementation.
 */
final class XdsClientImpl extends XdsClient
    implements XdsResponseHandler, ResourceStore, TimerLaunch {

  private static boolean LOG_XDS_NODE_ID = Boolean.parseBoolean(
      System.getenv("GRPC_LOG_XDS_NODE_ID"));
  private static final Logger classLogger = Logger.getLogger(XdsClientImpl.class.getName());

  // Longest time to wait, since the subscription to some resource, for concluding its absence.
  @VisibleForTesting
  static final int INITIAL_RESOURCE_FETCH_TIMEOUT_SEC = 15;
  private final SynchronizationContext syncContext = new SynchronizationContext(
      new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
//          logger.log(
//              XdsLogLevel.ERROR,
//              "Uncaught exception in XdsClient SynchronizationContext. Panic!",
//              e);
          // TODO(chengyuanzhang): better error handling.
          throw new AssertionError(e);
        }
      });
  private final FilterRegistry filterRegistry = FilterRegistry.getDefaultRegistry();
  private final LoadBalancerRegistry loadBalancerRegistry
      = LoadBalancerRegistry.getDefaultRegistry();
  private final Map<ServerInfo, ControlPlaneClient> serverChannelMap = new HashMap<>();
  private final Map<XdsResourceType<? extends ResourceUpdate>,
      Map<String, ResourceSubscriber<? extends ResourceUpdate>>>
      resourceSubscribers = new HashMap<>();
  private final Map<String, XdsResourceType<?>> subscribedResourceTypeUrls = new HashMap<>();
  private final Map<ServerInfo, LoadStatsManager2> loadStatsManagerMap = new HashMap<>();
  private final Map<ServerInfo, LoadReportClient> serverLrsClientMap = new HashMap<>();
  private final XdsChannelFactory xdsChannelFactory;
  private final Bootstrapper.BootstrapInfo bootstrapInfo;
  private final Context context;
  private final ScheduledExecutorService timeService;
  private final BackoffPolicy.Provider backoffPolicyProvider;
  private final Supplier<Stopwatch> stopwatchSupplier;
  private final TimeProvider timeProvider;
  private final TlsContextManager tlsContextManager;
  private final InternalLogId logId;
//  private final XdsLogger logger;
  private volatile boolean isShutdown;

  XdsClientImpl(
      XdsChannelFactory xdsChannelFactory,
      Bootstrapper.BootstrapInfo bootstrapInfo,
      Context context,
      ScheduledExecutorService timeService,
      BackoffPolicy.Provider backoffPolicyProvider,
      Supplier<Stopwatch> stopwatchSupplier,
      TimeProvider timeProvider,
      TlsContextManager tlsContextManager) {
    this.xdsChannelFactory = xdsChannelFactory;
    this.bootstrapInfo = bootstrapInfo;
    this.context = context;
    this.timeService = timeService;
    this.backoffPolicyProvider = backoffPolicyProvider;
    this.stopwatchSupplier = stopwatchSupplier;
    this.timeProvider = timeProvider;
    this.tlsContextManager = checkNotNull(tlsContextManager, "tlsContextManager");
    logId = InternalLogId.allocate("xds-client", null);
//    logger = XdsLogger.withLogId(logId);
//    logger.log(XdsLogLevel.INFO, "Created");
    if (LOG_XDS_NODE_ID) {
      classLogger.log(Level.INFO, "xDS node ID: {0}", bootstrapInfo.node().getId());
    }
  }

  private void maybeCreateXdsChannelWithLrs(ServerInfo serverInfo) {
    syncContext.throwIfNotInThisSynchronizationContext();
    if (serverChannelMap.containsKey(serverInfo)) {
      return;
    }
    ControlPlaneClient xdsChannel = new ControlPlaneClient(
        xdsChannelFactory,
        serverInfo,
        bootstrapInfo.node(),
        this,
        this,
        context,
        timeService,
        syncContext,
        backoffPolicyProvider,
        stopwatchSupplier,
        this);
    LoadStatsManager2 loadStatsManager = new LoadStatsManager2(stopwatchSupplier);
    loadStatsManagerMap.put(serverInfo, loadStatsManager);
    LoadReportClient lrsClient = new LoadReportClient(
        loadStatsManager, xdsChannel.channel(), context, bootstrapInfo.node(), syncContext,
        timeService, backoffPolicyProvider, stopwatchSupplier);
    serverChannelMap.put(serverInfo, xdsChannel);
    serverLrsClientMap.put(serverInfo, lrsClient);
  }

  @Override
  public void handleResourceResponse(
      XdsResourceType<?> xdsResourceType, ServerInfo serverInfo, String versionInfo,
      List<Any> resources, String nonce, ProcessingTracker processingTracker) {
    checkNotNull(xdsResourceType, "xdsResourceType");
    syncContext.throwIfNotInThisSynchronizationContext();
    Set<String> toParseResourceNames = null;
    if (!(xdsResourceType == XdsListenerResource.getInstance()
        || xdsResourceType == XdsRouteConfigureResource.getInstance())
        && resourceSubscribers.containsKey(xdsResourceType)) {
      toParseResourceNames = resourceSubscribers.get(xdsResourceType).keySet();
    }
    XdsResourceType.Args args = new XdsResourceType.Args(serverInfo, versionInfo, nonce,
        bootstrapInfo, filterRegistry, loadBalancerRegistry, tlsContextManager,
        toParseResourceNames);
    handleResourceUpdate(args, resources, xdsResourceType, processingTracker);
  }

  @Override
  public void handleStreamClosed(Status error) {
    syncContext.throwIfNotInThisSynchronizationContext();
    cleanUpResourceTimers();
    for (Map<String, ResourceSubscriber<? extends ResourceUpdate>> subscriberMap :
        resourceSubscribers.values()) {
      for (ResourceSubscriber<? extends ResourceUpdate> subscriber : subscriberMap.values()) {
        if (!subscriber.hasResult()) {
          subscriber.onError(error, null);
        }
      }
    }
  }

  @Override
  public void handleStreamRestarted(ServerInfo serverInfo) {
    syncContext.throwIfNotInThisSynchronizationContext();
    for (Map<String, ResourceSubscriber<? extends ResourceUpdate>> subscriberMap :
        resourceSubscribers.values()) {
      for (ResourceSubscriber<? extends ResourceUpdate> subscriber : subscriberMap.values()) {
        if (subscriber.serverInfo.equals(serverInfo)) {
          subscriber.restartTimer();
        }
      }
    }
  }

  @Override
  void shutdown() {
    syncContext.execute(
        new Runnable() {
          @Override
          public void run() {
            if (isShutdown) {
              return;
            }
            isShutdown = true;
            for (ControlPlaneClient xdsChannel : serverChannelMap.values()) {
              xdsChannel.shutdown();
            }
            for (final LoadReportClient lrsClient : serverLrsClientMap.values()) {
              lrsClient.stopLoadReporting();
            }
            cleanUpResourceTimers();
          }
        });
  }

  @Override
  boolean isShutDown() {
    return isShutdown;
  }

  @Override
  public Map<String, XdsResourceType<?>> getSubscribedResourceTypesWithTypeUrl() {
    return Collections.unmodifiableMap(subscribedResourceTypeUrls);
  }

  @Nullable
  @Override
  public Collection<String> getSubscribedResources(ServerInfo serverInfo,
                                                   XdsResourceType<? extends ResourceUpdate> type) {
    Map<String, ResourceSubscriber<? extends ResourceUpdate>> resources =
        resourceSubscribers.getOrDefault(type, Collections.emptyMap());
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (String key : resources.keySet()) {
      if (resources.get(key).serverInfo.equals(serverInfo)) {
        builder.add(key);
      }
    }
    Collection<String> retVal = builder.build();
    return retVal.isEmpty() ? null : retVal;
  }

  // As XdsClient APIs becomes resource agnostic, subscribed resource types are dynamic.
  // ResourceTypes that do not have subscribers does not show up in the snapshot keys.
  @Override
  ListenableFuture<Map<XdsResourceType<?>, Map<String, ResourceMetadata>>>
      getSubscribedResourcesMetadataSnapshot() {
    final SettableFuture<Map<XdsResourceType<?>, Map<String, ResourceMetadata>>> future =
        SettableFuture.create();
    syncContext.execute(new Runnable() {
      @Override
      public void run() {
        // A map from a "resource type" to a map ("resource name": "resource metadata")
        ImmutableMap.Builder<XdsResourceType<?>, Map<String, ResourceMetadata>> metadataSnapshot =
            ImmutableMap.builder();
        for (XdsResourceType<?> resourceType: resourceSubscribers.keySet()) {
          ImmutableMap.Builder<String, ResourceMetadata> metadataMap = ImmutableMap.builder();
          for (Map.Entry<String, ResourceSubscriber<? extends ResourceUpdate>> resourceEntry
              : resourceSubscribers.get(resourceType).entrySet()) {
            metadataMap.put(resourceEntry.getKey(), resourceEntry.getValue().metadata);
          }
          metadataSnapshot.put(resourceType, metadataMap.buildOrThrow());
        }
        future.set(metadataSnapshot.buildOrThrow());
      }
    });
    return future;
  }

  @Override
  TlsContextManager getTlsContextManager() {
    return tlsContextManager;
  }

  @Override
  <T extends ResourceUpdate> void watchXdsResource(XdsResourceType<T> type, String resourceName,
                                                   ResourceWatcher<T> watcher,
                                                   Executor watcherExecutor) {
    syncContext.execute(new Runnable() {
      @Override
      @SuppressWarnings("unchecked")
      public void run() {
        if (!resourceSubscribers.containsKey(type)) {
          resourceSubscribers.put(type, new HashMap<>());
          subscribedResourceTypeUrls.put(type.typeUrl(), type);
        }
        ResourceSubscriber<T> subscriber =
            (ResourceSubscriber<T>) resourceSubscribers.get(type).get(resourceName);
        if (subscriber == null) {
//          logger.log(XdsLogLevel.INFO, "Subscribe {0} resource {1}", type, resourceName);
          subscriber = new ResourceSubscriber<>(type, resourceName);
          resourceSubscribers.get(type).put(resourceName, subscriber);
          if (subscriber.xdsChannel != null) {
            subscriber.xdsChannel.adjustResourceSubscription(type);
          }
        }
        subscriber.addWatcher(watcher, watcherExecutor);
      }
    });
  }

  @Override
  <T extends ResourceUpdate> void cancelXdsResourceWatch(XdsResourceType<T> type,
                                                         String resourceName,
                                                         ResourceWatcher<T> watcher) {
    syncContext.execute(new Runnable() {
      @Override
      @SuppressWarnings("unchecked")
      public void run() {
        ResourceSubscriber<T> subscriber =
            (ResourceSubscriber<T>) resourceSubscribers.get(type).get(resourceName);;
        subscriber.removeWatcher(watcher);
        if (!subscriber.isWatched()) {
          subscriber.cancelResourceWatch();
          resourceSubscribers.get(type).remove(resourceName);
          if (subscriber.xdsChannel != null) {
            subscriber.xdsChannel.adjustResourceSubscription(type);
          }
          if (resourceSubscribers.get(type).isEmpty()) {
            resourceSubscribers.remove(type);
            subscribedResourceTypeUrls.remove(type.typeUrl());
          }
        }
      }
    });
  }

  @Override
  ClusterDropStats addClusterDropStats(
          final ServerInfo serverInfo, String clusterName, @Nullable String edsServiceName) {
    LoadStatsManager2 loadStatsManager = loadStatsManagerMap.get(serverInfo);
    ClusterDropStats dropCounter =
        loadStatsManager.getClusterDropStats(clusterName, edsServiceName);
    syncContext.execute(new Runnable() {
      @Override
      public void run() {
        serverLrsClientMap.get(serverInfo).startLoadReporting();
      }
    });
    return dropCounter;
  }

  @Override
  ClusterLocalityStats addClusterLocalityStats(
      final ServerInfo serverInfo, String clusterName, @Nullable String edsServiceName,
      Locality locality) {
    LoadStatsManager2 loadStatsManager = loadStatsManagerMap.get(serverInfo);
    ClusterLocalityStats loadCounter =
        loadStatsManager.getClusterLocalityStats(clusterName, edsServiceName, locality);
    syncContext.execute(new Runnable() {
      @Override
      public void run() {
        serverLrsClientMap.get(serverInfo).startLoadReporting();
      }
    });
    return loadCounter;
  }

  @Override
  Bootstrapper.BootstrapInfo getBootstrapInfo() {
    return bootstrapInfo;
  }

  @VisibleForTesting
  @Override
  Map<ServerInfo, LoadReportClient> getServerLrsClientMap() {
    return ImmutableMap.copyOf(serverLrsClientMap);
  }

  @Override
  public String toString() {
    return logId.toString();
  }

  @Override
  public void startSubscriberTimersIfNeeded(ServerInfo serverInfo) {
    if (isShutDown()) {
      return;
    }

    syncContext.execute(new Runnable() {
      @Override
      public void run() {
        if (isShutDown()) {
          return;
        }

        for (Map<String, ResourceSubscriber<?>> subscriberMap : resourceSubscribers.values()) {
          for (ResourceSubscriber<?> subscriber : subscriberMap.values()) {
            if (subscriber.serverInfo.equals(serverInfo) && subscriber.respTimer == null) {
              subscriber.restartTimer();
            }
          }
        }
      }
    });
  }

  private void cleanUpResourceTimers() {
    for (Map<String, ResourceSubscriber<?>> subscriberMap : resourceSubscribers.values()) {
      for (ResourceSubscriber<?> subscriber : subscriberMap.values()) {
        subscriber.stopTimer();
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends ResourceUpdate> void handleResourceUpdate(
      XdsResourceType.Args args, List<Any> resources, XdsResourceType<T> xdsResourceType,
      ProcessingTracker processingTracker) {
    ValidatedResourceUpdate<T> result = xdsResourceType.parse(args, resources);
//    logger.log(XdsLogger.XdsLogLevel.INFO,
//        "Received {0} Response version {1} nonce {2}. Parsed resources: {3}",
//         xdsResourceType.typeName(), args.versionInfo, args.nonce, result.unpackedResources);
    Map<String, ParsedResource<T>> parsedResources = result.parsedResources;
    Set<String> invalidResources = result.invalidResources;
    List<String> errors = result.errors;
    String errorDetail = null;
    if (errors.isEmpty()) {
      checkArgument(invalidResources.isEmpty(), "found invalid resources but missing errors");
      serverChannelMap.get(args.serverInfo).ackResponse(xdsResourceType, args.versionInfo,
          args.nonce);
    } else {
      errorDetail = Joiner.on('\n').join(errors);
//      logger.log(XdsLogLevel.WARNING,
//          "Failed processing {0} Response version {1} nonce {2}. Errors:\n{3}",
//          xdsResourceType.typeName(), args.versionInfo, args.nonce, errorDetail);
      serverChannelMap.get(args.serverInfo).nackResponse(xdsResourceType, args.nonce, errorDetail);
    }

    long updateTime = timeProvider.currentTimeNanos();
    Map<String, ResourceSubscriber<? extends ResourceUpdate>> subscribedResources =
        resourceSubscribers.getOrDefault(xdsResourceType, Collections.emptyMap());
    for (Map.Entry<String, ResourceSubscriber<?>> entry : subscribedResources.entrySet()) {
      String resourceName = entry.getKey();
      ResourceSubscriber<T> subscriber = (ResourceSubscriber<T>) entry.getValue();
      if (parsedResources.containsKey(resourceName)) {
        // Happy path: the resource updated successfully. Notify the watchers of the update.
        subscriber.onData(parsedResources.get(resourceName), args.versionInfo, updateTime,
            processingTracker);
        continue;
      }

      if (invalidResources.contains(resourceName)) {
        // The resource update is invalid. Capture the error without notifying the watchers.
        subscriber.onRejected(args.versionInfo, updateTime, errorDetail);
      }

      // Nothing else to do for incremental ADS resources.
      if (!xdsResourceType.isFullStateOfTheWorld()) {
        continue;
      }

      // Handle State of the World ADS: invalid resources.
      if (invalidResources.contains(resourceName)) {
        // The resource is missing. Reuse the cached resource if possible.
        if (subscriber.data == null) {
          // No cached data. Notify the watchers of an invalid update.
          subscriber.onError(Status.UNAVAILABLE.withDescription(errorDetail), processingTracker);
        }
        continue;
      }

      // For State of the World services, notify watchers when their watched resource is missing
      // from the ADS update. Note that we can only do this if the resource update is coming from
      // the same xDS server that the ResourceSubscriber is subscribed to.
      if (subscriber.serverInfo.equals(args.serverInfo)) {
        subscriber.onAbsent(processingTracker);
      }
    }
  }

  /**
   * Tracks a single subscribed resource.
   */
  private final class ResourceSubscriber<T extends ResourceUpdate> {
    @Nullable private final ServerInfo serverInfo;
    @Nullable private final ControlPlaneClient xdsChannel;
    private final XdsResourceType<T> type;
    private final String resource;
    private final Map<ResourceWatcher<T>, Executor> watchers = new HashMap<>();
    @Nullable private T data;
    private boolean absent;
    // Tracks whether the deletion has been ignored per bootstrap server feature.
    // See https://github.com/grpc/proposal/blob/master/A53-xds-ignore-resource-deletion.md
    private boolean resourceDeletionIgnored;
    @Nullable private ScheduledHandle respTimer;
    @Nullable private ResourceMetadata metadata;
    @Nullable private String errorDescription;

    ResourceSubscriber(XdsResourceType<T> type, String resource) {
      syncContext.throwIfNotInThisSynchronizationContext();
      this.type = type;
      this.resource = resource;
      this.serverInfo = getServerInfo(resource);
      if (serverInfo == null) {
        this.errorDescription = "Wrong configuration: xds server does not exist for resource "
            + resource;
        this.xdsChannel = null;
        return;
      }
      // Initialize metadata in UNKNOWN state to cover the case when resource subscriber,
      // is created but not yet requested because the client is in backoff.
      this.metadata = ResourceMetadata.newResourceMetadataUnknown();

      ControlPlaneClient xdsChannelTemp = null;
      try {
        maybeCreateXdsChannelWithLrs(serverInfo);
        xdsChannelTemp = serverChannelMap.get(serverInfo);
        if (xdsChannelTemp.isInBackoff()) {
          return;
        }
      } catch (IllegalArgumentException e) {
        xdsChannelTemp = null;
        this.errorDescription = "Bad configuration:  " + e.getMessage();
        return;
      } finally {
        this.xdsChannel = xdsChannelTemp;
      }

      restartTimer();
    }

    @Nullable
    private ServerInfo getServerInfo(String resource) {
      if (BootstrapperImpl.enableFederation && resource.startsWith(XDSTP_SCHEME)) {
        URI uri = URI.create(resource);
        String authority = uri.getAuthority();
        if (authority == null) {
          authority = "";
        }
        AuthorityInfo authorityInfo = bootstrapInfo.authorities().get(authority);
        if (authorityInfo == null || authorityInfo.xdsServers().isEmpty()) {
          return null;
        }
        return authorityInfo.xdsServers().get(0);
      }
      return bootstrapInfo.servers().get(0); // use first server
    }

    void addWatcher(ResourceWatcher<T> watcher, Executor watcherExecutor) {
      checkArgument(!watchers.containsKey(watcher), "watcher %s already registered", watcher);
      watchers.put(watcher, watcherExecutor);
      T savedData = data;
      boolean savedAbsent = absent;
      watcherExecutor.execute(() -> {
        if (errorDescription != null) {
          watcher.onError(Status.INVALID_ARGUMENT.withDescription(errorDescription));
          return;
        }
        if (savedData != null) {
          notifyWatcher(watcher, savedData);
        } else if (savedAbsent) {
          watcher.onResourceDoesNotExist(resource);
        }
      });
    }

    void removeWatcher(ResourceWatcher<T>  watcher) {
      checkArgument(watchers.containsKey(watcher), "watcher %s not registered", watcher);
      watchers.remove(watcher);
    }

    void restartTimer() {
      if (data != null || absent) {  // resource already resolved
        return;
      }
      if (!xdsChannel.isReady()) { // When channel becomes ready, it will trigger a restartTimer
        return;
      }

      class ResourceNotFound implements Runnable {
        @Override
        public void run() {
//          logger.log(XdsLogLevel.INFO, "{0} resource {1} initial fetch timeout",
//              type, resource);
          respTimer = null;
          onAbsent(null);
        }

        @Override
        public String toString() {
          return type + this.getClass().getSimpleName();
        }
      }

      // Initial fetch scheduled or rescheduled, transition metadata state to REQUESTED.
      metadata = ResourceMetadata.newResourceMetadataRequested();

      respTimer = syncContext.schedule(
          new ResourceNotFound(), INITIAL_RESOURCE_FETCH_TIMEOUT_SEC, TimeUnit.SECONDS,
          timeService);
    }

    void stopTimer() {
      if (respTimer != null && respTimer.isPending()) {
        respTimer.cancel();
        respTimer = null;
      }
    }

    void cancelResourceWatch() {
      if (isWatched()) {
        throw new IllegalStateException("Can't cancel resource watch with active watchers present");
      }
      stopTimer();
      String message = "Unsubscribing {0} resource {1} from server {2}";
//      XdsLogLevel logLevel = XdsLogLevel.INFO;
      if (resourceDeletionIgnored) {
        message += " for which we previously ignored a deletion";
//        logLevel = XdsLogLevel.FORCE_INFO;
      }
//      logger.log(logLevel, message, type, resource,
//          serverInfo != null ? serverInfo.target() : "unknown");
    }

    boolean isWatched() {
      return !watchers.isEmpty();
    }

    boolean hasResult() {
      return data != null || absent;
    }

    void onData(ParsedResource<T> parsedResource, String version, long updateTime,
                ProcessingTracker processingTracker) {
      if (respTimer != null && respTimer.isPending()) {
        respTimer.cancel();
        respTimer = null;
      }
      this.metadata = ResourceMetadata
          .newResourceMetadataAcked(parsedResource.getRawResource(), version, updateTime);
      ResourceUpdate oldData = this.data;
      this.data = parsedResource.getResourceUpdate();
      absent = false;
      if (resourceDeletionIgnored) {
//        logger.log(XdsLogLevel.FORCE_INFO, "xds server {0}: server returned new version "
//                + "of resource for which we previously ignored a deletion: type {1} name {2}",
//            serverInfo != null ? serverInfo.target() : "unknown", type, resource);
        resourceDeletionIgnored = false;
      }
      if (!Objects.equals(oldData, data)) {
        for (ResourceWatcher<T> watcher : watchers.keySet()) {
          processingTracker.startTask();
          watchers.get(watcher).execute(() -> {
            try {
              notifyWatcher(watcher, data);
            } finally {
              processingTracker.onComplete();
            }
          });
        }
      }
    }

    void onAbsent(@Nullable ProcessingTracker processingTracker) {
      if (respTimer != null && respTimer.isPending()) {  // too early to conclude absence
        return;
      }

      // Ignore deletion of State of the World resources when this feature is on,
      // and the resource is reusable.
      boolean ignoreResourceDeletionEnabled =
          serverInfo != null && serverInfo.ignoreResourceDeletion();
      if (ignoreResourceDeletionEnabled && type.isFullStateOfTheWorld() && data != null) {
        if (!resourceDeletionIgnored) {
//          logger.log(XdsLogLevel.FORCE_WARNING,
//              "xds server {0}: ignoring deletion for resource type {1} name {2}}",
//              serverInfo.target(), type, resource);
          resourceDeletionIgnored = true;
        }
        return;
      }

//      logger.log(XdsLogLevel.INFO, "Conclude {0} resource {1} not exist", type, resource);
      if (!absent) {
        data = null;
        absent = true;
        metadata = ResourceMetadata.newResourceMetadataDoesNotExist();
        for (ResourceWatcher<T> watcher : watchers.keySet()) {
          if (processingTracker != null) {
            processingTracker.startTask();
          }
          watchers.get(watcher).execute(() -> {
            try {
              watcher.onResourceDoesNotExist(resource);
            } finally {
              if (processingTracker != null) {
                processingTracker.onComplete();
              }
            }
          });
        }
      }
    }

    void onError(Status error, @Nullable ProcessingTracker tracker) {
      if (respTimer != null && respTimer.isPending()) {
        respTimer.cancel();
        respTimer = null;
      }

      // Include node ID in xds failures to allow cross-referencing with control plane logs
      // when debugging.
      String description = error.getDescription() == null ? "" : error.getDescription() + " ";
      Status errorAugmented = Status.fromCode(error.getCode())
          .withDescription(description + "nodeID: " + bootstrapInfo.node().getId())
          .withCause(error.getCause());

      for (ResourceWatcher<T> watcher : watchers.keySet()) {
        if (tracker != null) {
          tracker.startTask();
        }
        watchers.get(watcher).execute(() -> {
          try {
            watcher.onError(errorAugmented);
          } finally {
            if (tracker != null) {
              tracker.onComplete();
            }
          }
        });
      }
    }

    void onRejected(String rejectedVersion, long rejectedTime, String rejectedDetails) {
      metadata = ResourceMetadata
          .newResourceMetadataNacked(metadata, rejectedVersion, rejectedTime, rejectedDetails);
    }

    private void notifyWatcher(ResourceWatcher<T> watcher, T update) {
      watcher.onChanged(update);
    }
  }

  static final class ResourceInvalidException extends Exception {
    private static final long serialVersionUID = 0L;

    ResourceInvalidException(String message) {
      super(message, null, false, false);
    }

    ResourceInvalidException(String message, Throwable cause) {
      super(cause != null ? message + ": " + cause.getMessage() : message, cause, false, false);
    }
  }

  abstract static class XdsChannelFactory {
    static final XdsChannelFactory DEFAULT_XDS_CHANNEL_FACTORY = new XdsChannelFactory() {
      @Override
      ManagedChannel create(ServerInfo serverInfo) {
        String target = serverInfo.target();
        ChannelCredentials channelCredentials = serverInfo.channelCredentials();
        return Grpc.newChannelBuilder(target, channelCredentials)
            .keepAliveTime(5, TimeUnit.MINUTES)
            .build();
      }
    };

    abstract ManagedChannel create(ServerInfo serverInfo);
  }
}
