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
package org.apache.dubbo.metadata.definition.protobuf;

import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.TypeDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;
import org.apache.dubbo.metadata.definition.protobuf.model.ServiceInterface;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 2019-07-01
 */
class ProtobufTypeBuilderTest {
    @Test
    void testProtobufBuilder() {
        TypeDefinitionBuilder.initBuilders(FrameworkModel.defaultModel());

        // TEST Pb Service metaData builder
        FullServiceDefinition serviceDefinition = ServiceDefinitionBuilder.buildFullDefinition(ServiceInterface.class);
        MethodDefinition methodDefinition = serviceDefinition.getMethods().get(0);
        List<TypeDefinition> types = serviceDefinition.getTypes();
        String parameterName = methodDefinition.getParameterTypes()[0];
        TypeDefinition typeDefinition = null;
        for (TypeDefinition type : serviceDefinition.getTypes()) {
            if (parameterName.equals(type.getType())) {
                typeDefinition = type;
                break;
            }
        }
        Map<String, String> propertiesMap = typeDefinition.getProperties();
        Assertions.assertEquals(11, propertiesMap.size());
        Assertions.assertTrue(propertiesMap.containsKey("money"));
        Assertions.assertEquals("double", getTypeName(propertiesMap.get("money"), types));
        Assertions.assertTrue(propertiesMap.containsKey("cash"));
        Assertions.assertEquals("float", getTypeName(propertiesMap.get("cash"), types));
        Assertions.assertTrue(propertiesMap.containsKey("age"));
        Assertions.assertEquals("int", getTypeName(propertiesMap.get("age"), types));
        Assertions.assertTrue(propertiesMap.containsKey("num"));
        Assertions.assertEquals("long", getTypeName(propertiesMap.get("num"), types));
        Assertions.assertTrue(propertiesMap.containsKey("sex"));
        Assertions.assertEquals("boolean", getTypeName(propertiesMap.get("sex"), types));
        Assertions.assertTrue(propertiesMap.containsKey("name"));
        Assertions.assertEquals("java.lang.String", getTypeName(propertiesMap.get("name"), types));
        Assertions.assertTrue(propertiesMap.containsKey("msg"));
        Assertions.assertEquals("com.google.protobuf.ByteString", getTypeName(propertiesMap.get("msg"), types));
        Assertions.assertTrue(propertiesMap.containsKey("phone"));
        Assertions.assertEquals(
                "java.util.List<org.apache.dubbo.metadata.definition.protobuf.model.GooglePB.PhoneNumber>",
                getTypeName(propertiesMap.get("phone"), types));
        Assertions.assertTrue(propertiesMap.containsKey("doubleMap"));
        Assertions.assertEquals(
                "java.util.Map<java.lang.String,org.apache.dubbo.metadata.definition.protobuf.model.GooglePB.PhoneNumber>",
                getTypeName(propertiesMap.get("doubleMap"), types));
        Assertions.assertEquals(
                "java.util.List<com.google.protobuf.ByteString>", getTypeName(propertiesMap.get("bytesList"), types));
        Assertions.assertEquals(
                "java.util.Map<java.lang.String,com.google.protobuf.ByteString>",
                getTypeName(propertiesMap.get("bytesMap"), types));
    }

    private static String getTypeName(String type, List<TypeDefinition> types) {
        for (TypeDefinition typeDefinition : types) {
            if (type.equals(typeDefinition.getType())) {
                return typeDefinition.getType();
            }
        }
        return type;
    }
}
