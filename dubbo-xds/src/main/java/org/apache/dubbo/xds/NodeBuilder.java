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
package org.apache.dubbo.xds;

import org.apache.dubbo.xds.bootstrap.BootstrapInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;

import java.util.List;
import java.util.Map;

import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.envoyproxy.envoy.config.core.v3.Node;

public class NodeBuilder {

    public static Node build() {
        BootstrapInfo bootstrapInfo = Bootstrapper.getInstance().bootstrap();
        return Node.newBuilder()
                .setMetadata(mapToStruct(bootstrapInfo.getNode().getMetadata()))
                .setId(bootstrapInfo.getNode().getId())
                .setCluster(bootstrapInfo.getNode().getCluster())
                .build();
    }

    public static Struct mapToStruct(Map<String, ?> map) {
        Struct.Builder structBuilder = Struct.newBuilder();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            structBuilder.putFields(entry.getKey(), toValue(entry.getValue()));
        }
        return structBuilder.build();
    }

    private static Value toValue(Object obj) {
        if (obj == null) {
            return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
        } else if (obj instanceof String) {
            return Value.newBuilder().setStringValue((String) obj).build();
        } else if (obj instanceof Number) {
            return Value.newBuilder()
                    .setNumberValue(((Number) obj).doubleValue())
                    .build();
        } else if (obj instanceof Boolean) {
            return Value.newBuilder().setBoolValue((Boolean) obj).build();
        } else if (obj instanceof Map) {
            Map<?, ?> mapObj = (Map<?, ?>) obj;
            Struct.Builder nestedStruct = Struct.newBuilder();
            for (Map.Entry<?, ?> e : mapObj.entrySet()) {
                if (e.getKey() instanceof String) {
                    nestedStruct.putFields((String) e.getKey(), toValue(e.getValue()));
                }
            }
            return Value.newBuilder().setStructValue(nestedStruct.build()).build();
        } else if (obj instanceof List) {
            List<?> listObj = (List<?>) obj;
            ListValue.Builder listValue = ListValue.newBuilder();
            for (Object element : listObj) {
                listValue.addValues(toValue(element));
            }
            return Value.newBuilder().setListValue(listValue.build()).build();
        } else {
            return Value.newBuilder().setStringValue(obj.toString()).build();
        }
    }
}
