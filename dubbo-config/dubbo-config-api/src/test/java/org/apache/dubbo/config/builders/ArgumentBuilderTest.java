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
package org.apache.dubbo.config.builders;

import org.apache.dubbo.config.ArgumentConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ArgumentBuilderTest {

    @Test
    void index() {
        ArgumentBuilder builder = new ArgumentBuilder();
        builder.index(1);
        Assertions.assertEquals(1, builder.build().getIndex());
    }

    @Test
    void type() {
        ArgumentBuilder builder = new ArgumentBuilder();
        builder.type("int");
        Assertions.assertEquals("int", builder.build().getType());
    }

    @Test
    void callback() {
        ArgumentBuilder builder = new ArgumentBuilder();
        builder.callback(true);
        Assertions.assertTrue(builder.build().isCallback());
        builder.callback(false);
        Assertions.assertFalse(builder.build().isCallback());
    }

    @Test
    void build() {
        ArgumentBuilder builder = new ArgumentBuilder();
        builder.index(1).type("int").callback(true);

        ArgumentConfig argument1 = builder.build();
        ArgumentConfig argument2 = builder.build();

        Assertions.assertTrue(argument1.isCallback());
        Assertions.assertEquals("int", argument1.getType());
        Assertions.assertEquals(1, argument1.getIndex());

        Assertions.assertNotSame(argument1, argument2);
    }
}