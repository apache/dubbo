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

package org.apache.dubbo.xds.resource_new.route.plugin;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.xds.resource_new.common.ConfigOrError;
import org.apache.dubbo.xds.resource_new.common.MessagePrinter;

import java.util.Map;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * The ClusterSpecifierPlugin for RouteLookup policy.
 */
final class RouteLookupServiceClusterSpecifierPlugin implements ClusterSpecifierPlugin {

    static final RouteLookupServiceClusterSpecifierPlugin INSTANCE = new RouteLookupServiceClusterSpecifierPlugin();

    private static final String TYPE_URL = "type.googleapis.com/grpc.lookup.v1.RouteLookupClusterSpecifier";

    private RouteLookupServiceClusterSpecifierPlugin() {}

    @Override
    public String[] typeUrls() {
        return new String[] {TYPE_URL,};
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
                protoClass = (Class<? extends Message>) Class.forName("io.grpc.lookup.v1.RouteLookupClusterSpecifier");
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
            Map<String, ?> jsonMap = JsonUtils.toJavaObject(jsonString, Map.class);
            Map<String, ?> config = JsonUtils.toJavaObject(jsonMap.get("routeLookupConfig")
                    .toString(), Map.class);
            return ConfigOrError.fromConfig(RlsPluginConfig.create(config));
        } catch (RuntimeException e) {
            return ConfigOrError.fromError("Error parsing RouteLookupConfig: " + e);
        }
    }
}
