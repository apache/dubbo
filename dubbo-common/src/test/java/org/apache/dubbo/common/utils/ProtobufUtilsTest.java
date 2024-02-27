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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.vo.UserVo;
import org.apache.dubbo.rpc.model.HelloReply;
import org.apache.dubbo.rpc.model.HelloRequest;
import org.apache.dubbo.rpc.model.Person;
import org.apache.dubbo.rpc.model.SerializablePerson;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProtobufUtilsTest {

    @Test
    void testIsProtobufClass() {
        Assertions.assertTrue(ProtobufUtils.isProtobufClass(HelloRequest.class));
        Assertions.assertTrue(ProtobufUtils.isProtobufClass(HelloReply.class));
        Assertions.assertFalse(ProtobufUtils.isProtobufClass(Person.class));
        Assertions.assertFalse(ProtobufUtils.isProtobufClass(SerializablePerson.class));
        Assertions.assertFalse(ProtobufUtils.isProtobufClass(UserVo.class));
    }
}
