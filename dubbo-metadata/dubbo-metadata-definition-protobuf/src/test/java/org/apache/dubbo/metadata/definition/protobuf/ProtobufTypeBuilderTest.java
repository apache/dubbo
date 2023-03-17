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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(propertiesMap.size(), is(11));
        assertThat(propertiesMap.containsKey("money"), is(true));
        assertThat(getTypeName(propertiesMap.get("money"), types), equalTo("double"));
        assertThat(propertiesMap.containsKey("cash"), is(true));
        assertThat(getTypeName(propertiesMap.get("cash"), types), equalTo("float"));
        assertThat(propertiesMap.containsKey("age"), is(true));
        assertThat(getTypeName(propertiesMap.get("age"), types), equalTo("int"));
        assertThat(propertiesMap.containsKey("num"), is(true));
        assertThat(getTypeName(propertiesMap.get("num"), types), equalTo("long"));
        assertThat(propertiesMap.containsKey("sex"), is(true));
        assertThat(getTypeName(propertiesMap.get("sex"), types), equalTo("boolean"));
        assertThat(propertiesMap.containsKey("name"), is(true));
        assertThat(getTypeName(propertiesMap.get("name"), types), equalTo("java.lang.String"));
        assertThat(propertiesMap.containsKey("msg"), is(true));
        assertThat(getTypeName(propertiesMap.get("msg"), types),
                equalTo("com.google.protobuf.ByteString"));
        assertThat(propertiesMap.containsKey("phone"), is(true));
        assertThat(getTypeName(propertiesMap.get("phone"), types),
                equalTo("java.util.List<org.apache.dubbo.metadata.definition.protobuf.model.GooglePB.PhoneNumber>"));
        assertThat(propertiesMap.containsKey("doubleMap"), is(true));
        assertThat(getTypeName(propertiesMap.get("doubleMap"), types),
                equalTo("java.util.Map<java.lang.String,org.apache.dubbo.metadata.definition.protobuf.model.GooglePB.PhoneNumber>"));
        assertThat(getTypeName(propertiesMap.get("bytesList"), types),
                equalTo("java.util.List<com.google.protobuf.ByteString>"));
        assertThat(getTypeName(propertiesMap.get("bytesMap"), types),
                equalTo("java.util.Map<java.lang.String,com.google.protobuf.ByteString>"));
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
