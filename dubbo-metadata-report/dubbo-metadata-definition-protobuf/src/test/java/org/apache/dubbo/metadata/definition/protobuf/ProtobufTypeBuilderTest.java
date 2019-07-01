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
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;
import org.apache.dubbo.metadata.definition.protobuf.model.ServiceInterface;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 2019-07-01
 */
public class ProtobufTypeBuilderTest {
    @Test
    public void testProtobufBuilder() {
        // TEST Pb Service metaData builder
        FullServiceDefinition serviceDefinition = ServiceDefinitionBuilder.buildFullDefinition(ServiceInterface.class);
        MethodDefinition methodDefinition = serviceDefinition.getMethods().get(0);
        String parameterName = methodDefinition.getParameterTypes()[0];
        TypeDefinition typeDefinition = null;
        for (TypeDefinition type : serviceDefinition.getTypes()) {
            if (parameterName.equals(type.getType())) {
                typeDefinition = type;
                break;
            }
        }
        Map<String, TypeDefinition> propertiesMap = typeDefinition.getProperties();
        assertThat(propertiesMap.size(), is(9));
        assertThat(propertiesMap.containsKey("money"), is(true));
        assertThat(propertiesMap.get("money").getType(), equalTo("double"));
        assertThat(propertiesMap.containsKey("cash"), is(true));
        assertThat(propertiesMap.get("cash").getType(), equalTo("float"));
        assertThat(propertiesMap.containsKey("age"), is(true));
        assertThat(propertiesMap.get("age").getType(), equalTo("int"));
        assertThat(propertiesMap.containsKey("num"), is(true));
        assertThat(propertiesMap.get("num").getType(), equalTo("long"));
        assertThat(propertiesMap.containsKey("sex"), is(true));
        assertThat(propertiesMap.get("sex").getType(), equalTo("boolean"));
        assertThat(propertiesMap.containsKey("name"), is(true));
        assertThat(propertiesMap.get("name").getType(), equalTo("java.lang.String"));
        assertThat(propertiesMap.containsKey("msg"), is(true));
        assertThat(propertiesMap.get("msg").getType(), equalTo("com.google.protobuf.ByteString"));
        assertThat(propertiesMap.containsKey("phone"), is(true));
        assertThat(propertiesMap.get("phone").getType(), equalTo("java.util.List<org.apache.dubbo.metadata.definition.protobuf.model.GooglePB$PhoneNumber>"));
        assertThat(propertiesMap.containsKey("doubleMap"), is(true));
        assertThat(propertiesMap.get("doubleMap").getType(), equalTo("java.util.Map<java.lang.String, org.apache.dubbo.metadata.definition.protobuf.model.GooglePB$PhoneNumber>"));
    }
}
