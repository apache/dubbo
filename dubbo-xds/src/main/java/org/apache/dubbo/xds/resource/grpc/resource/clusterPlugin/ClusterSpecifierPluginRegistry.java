/*
 * Copyright 2021 The gRPC Authors
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

package org.apache.dubbo.xds.resource.grpc.resource.clusterPlugin;

import org.apache.dubbo.common.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ClusterSpecifierPluginRegistry {
  private static ClusterSpecifierPluginRegistry instance;

  private final Map<String, ClusterSpecifierPlugin> supportedPlugins = new HashMap<>();

  private ClusterSpecifierPluginRegistry() {}

  public static synchronized ClusterSpecifierPluginRegistry getDefaultRegistry() {
    if (instance == null) {
      instance = newRegistry().register(RouteLookupServiceClusterSpecifierPlugin.INSTANCE);
    }
    return instance;
  }

  static ClusterSpecifierPluginRegistry newRegistry() {
    return new ClusterSpecifierPluginRegistry();
  }

  ClusterSpecifierPluginRegistry register(ClusterSpecifierPlugin... plugins) {
    for (ClusterSpecifierPlugin plugin : plugins) {
      for (String typeUrl : plugin.typeUrls()) {
        supportedPlugins.put(typeUrl, plugin);
      }
    }
    return this;
  }

  @Nullable
  public ClusterSpecifierPlugin get(String typeUrl) {
    return supportedPlugins.get(typeUrl);
  }
}
