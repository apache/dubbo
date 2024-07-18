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

package org.apache.dubbo.xds.resource.grpc;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.internal.JsonParser;
import io.grpc.internal.JsonUtil;

import java.io.IOException;
import java.util.Map;

/** The ClusterSpecifierPlugin for RouteLookup policy. */
final class RouteLookupServiceClusterSpecifierPlugin implements ClusterSpecifierPlugin {

  static final RouteLookupServiceClusterSpecifierPlugin INSTANCE =
      new RouteLookupServiceClusterSpecifierPlugin();

  private static final String TYPE_URL =
      "type.googleapis.com/grpc.lookup.v1.RouteLookupClusterSpecifier";

  private RouteLookupServiceClusterSpecifierPlugin() {}

  @Override
  public String[] typeUrls() {
    return new String[] {
        TYPE_URL,
    };
  }

  @Override
  @SuppressWarnings("unchecked")
  public ConfigOrError<RlsPluginConfig> parsePlugin(Message rawProtoMessage) {
    if (!(rawProtoMessage instanceof Any)) {
      return ConfigOrError.fromError("Invalid config type: " + rawProtoMessage.getClass());
    }
    try {
      Any anyMessage = (Any) rawProtoMessage;
      Class<? extends Message> protoClass;
      try {
        protoClass =
            (Class<? extends Message>)
                Class.forName("io.grpc.lookup.v1.RouteLookupClusterSpecifier");
      } catch (ClassNotFoundException e) {
        return ConfigOrError.fromError("Dependency for 'io.grpc:grpc-rls' is missing: " + e);
      }
      Message configProto;
      try {
        configProto = anyMessage.unpack(protoClass);
      } catch (InvalidProtocolBufferException e) {
        return ConfigOrError.fromError("Invalid proto: " + e);
      }
      String jsonString = MessagePrinter.print(configProto);
      try {
        Map<String, ?> jsonMap = (Map<String, ?>) JsonParser.parse(jsonString);
        Map<String, ?> config = JsonUtil.getObject(jsonMap, "routeLookupConfig");
        return ConfigOrError.fromConfig(RlsPluginConfig.create(config));
      } catch (IOException e) {
        return ConfigOrError.fromError(
            "Unable to parse RouteLookupClusterSpecifier: " + jsonString);
      }
    } catch (RuntimeException e) {
      return ConfigOrError.fromError("Error parsing RouteLookupConfig: " + e);
    }
  }

  @AutoValue
  abstract static class RlsPluginConfig implements PluginConfig {

    abstract ImmutableMap<String, ?> config();

    static RlsPluginConfig create(Map<String, ?> config) {
      return new AutoValue_RouteLookupServiceClusterSpecifierPlugin_RlsPluginConfig(
          ImmutableMap.copyOf(config));
    }

    @Override
    public String typeUrl() {
      return TYPE_URL;
    }
  }
}
