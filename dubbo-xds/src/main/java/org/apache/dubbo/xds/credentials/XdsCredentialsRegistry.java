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

package org.apache.dubbo.xds.credentials;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.grpc.xds.internal.GoogleDefaultXdsCredentialsProvider;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Registry of {@link XdsCredentialsProvider}s. The {@link #getDefaultRegistry default
 * instance} loads providers at runtime through the Java service provider mechanism.
 */
@ThreadSafe
public final class XdsCredentialsRegistry {
  private static final Logger logger = Logger.getLogger(XdsCredentialsRegistry.class.getName());
  private static XdsCredentialsRegistry instance;

  @GuardedBy("this")
  private final LinkedHashSet<XdsCredentialsProvider> allProviders = new LinkedHashSet<>();

  /**
   * Generated from {@code allProviders}. Is mapping from scheme key to the
   * highest priority {@link XdsCredentialsProvider}.
   * Is replaced instead of mutating.
   */
  @GuardedBy("this")
  private ImmutableMap<String, XdsCredentialsProvider> effectiveProviders = ImmutableMap.of();

  /**
   * Register a provider.
   *
   * <p>If the provider's {@link XdsCredentialsProvider#isAvailable isAvailable()}
   * returns {@code false}, this method will throw {@link IllegalArgumentException}.
   *
   * <p>Providers will be used in priority order. In case of ties, providers are used
   * in registration order.
   */
  public synchronized void register(XdsCredentialsProvider provider) {
    addProvider(provider);
    refreshProviders();
  }

  private synchronized void addProvider(XdsCredentialsProvider provider) {
    checkArgument(provider.isAvailable(), "isAvailable() returned false");
    allProviders.add(provider);
  }

  /**
   * Deregisters a provider. No-op if the provider is not in the registry.
   *
   * @param provider the provider that was added to the register via
   *                 {@link #register}.
   */
  public synchronized void deregister(XdsCredentialsProvider provider) {
    allProviders.remove(provider);
    refreshProviders();
  }

  private synchronized void refreshProviders() {
    Map<String, XdsCredentialsProvider> refreshedProviders = new HashMap<>();
    int maxPriority = Integer.MIN_VALUE;
    // We prefer first-registered providers.
    for (XdsCredentialsProvider provider : allProviders) {
      String credsName = provider.getName();
      XdsCredentialsProvider existing = refreshedProviders.get(credsName);
      if (existing == null || existing.priority() < provider.priority()) {
        refreshedProviders.put(credsName, provider);
      }
      if (maxPriority < provider.priority()) {
        maxPriority = provider.priority();
      }
    }
    effectiveProviders = ImmutableMap.copyOf(refreshedProviders);
  }

  /**
   * Returns the default registry that loads providers via the Java service loader
   * mechanism.
   */
  public static synchronized XdsCredentialsRegistry getDefaultRegistry() {
    if (instance == null) {
      List<XdsCredentialsProvider> providerList = InternalServiceProviders.loadAll(
              XdsCredentialsProvider.class,
              getHardCodedClasses(),
              XdsCredentialsProvider.class.getClassLoader(),
              new XdsCredentialsProviderPriorityAccessor());
      if (providerList.isEmpty()) {
        logger.warning("No XdsCredsRegistry found via ServiceLoader, including for GoogleDefault, "
            + "TLS and Insecure. This is probably due to a broken build.");
      }
      instance = new XdsCredentialsRegistry();
      for (XdsCredentialsProvider provider : providerList) {
        logger.fine("Service loader found " + provider);
        if (provider.isAvailable()) {
          instance.addProvider(provider);
        }
      }
      instance.refreshProviders();
    }
    return instance;
  }

  /**
   * Returns effective providers map from scheme to the highest priority
   * XdsCredsProvider of that scheme.
   */
  @VisibleForTesting
  synchronized Map<String, XdsCredentialsProvider> providers() {
    return effectiveProviders;
  }

  /**
   * Returns the effective provider for the given xds credential name, or {@code null} if no
   * suitable provider can be found.
   * Each provider declares its name via {@link XdsCredentialsProvider#getName}.
   */
  @Nullable
  public synchronized XdsCredentialsProvider getProvider(String name) {
    return effectiveProviders.get(checkNotNull(name, "name"));
  }

  @VisibleForTesting
  static List<Class<?>> getHardCodedClasses() {
    // Class.forName(String) is used to remove the need for ProGuard configuration. Note that
    // ProGuard does not detect usages of Class.forName(String, boolean, ClassLoader):
    // https://sourceforge.net/p/proguard/bugs/418/
    ArrayList<Class<?>> list = new ArrayList<>();
    try {
      list.add(Class.forName("org.apache.dubbo.xds.credentials.GoogleDefaultXdsCredentialsProvider"));
        ClassLoader classLoader = GoogleDefaultXdsCredentialsProvider.class.getClassLoader();
        logger.info("ClassLoader: " + classLoader);
    } catch (ClassNotFoundException e) {
      logger.log(Level.WARNING, "Unable to find GoogleDefaultXdsCredentialsProvider", e);
    }

    try {
      list.add(Class.forName("org.apache.dubbo.xds.credentials.InsecureXdsCredentialsProvider"));
    }  catch (ClassNotFoundException e) {
      logger.log(Level.WARNING, "Unable to find InsecureXdsCredentialsProvider", e);
    }

    try {
      list.add(Class.forName("org.apache.dubbo.xds.credentials.TlsXdsCredentialsProvider"));
    } catch (ClassNotFoundException e) {
      logger.log(Level.WARNING, "Unable to find TlsXdsCredentialsProvider", e);
    }

    return Collections.unmodifiableList(list);
  }

  private static final class XdsCredentialsProviderPriorityAccessor
          implements InternalServiceProviders.PriorityAccessor<XdsCredentialsProvider> {
    @Override
    public boolean isAvailable(XdsCredentialsProvider provider) {
      return provider.isAvailable();
    }

    @Override
    public int getPriority(XdsCredentialsProvider provider) {
      return provider.priority();
    }
  }
}
