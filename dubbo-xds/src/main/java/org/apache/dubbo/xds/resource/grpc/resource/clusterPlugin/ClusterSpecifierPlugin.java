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

import com.google.protobuf.Message;

import org.apache.dubbo.xds.resource.grpc.resource.common.ConfigOrError;

/**
 * Defines the parsing functionality of a ClusterSpecifierPlugin as defined in the Enovy proto
 * api/envoy/config/route/v3/route.proto.
 */
public interface ClusterSpecifierPlugin {
  /**
   * The proto message types supported by this plugin. A plugin will be registered by each of its
   * supported message types.
   */
  String[] typeUrls();

  ConfigOrError<? extends PluginConfig> parsePlugin(Message rawProtoMessage);

}
