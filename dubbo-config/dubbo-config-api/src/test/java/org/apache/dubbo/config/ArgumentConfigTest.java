/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

class ArgumentConfigTest {
    @Test
    void testIndex() {
        ArgumentConfig argument = new ArgumentConfig();
        argument.setIndex(1);
        assertThat(argument.getIndex(), is(1));
    }

    @Test
    void testType() {
        ArgumentConfig argument = new ArgumentConfig();
        argument.setType("int");
        assertThat(argument.getType(), equalTo("int"));
    }

    @Test
    void testCallback() {
        ArgumentConfig argument = new ArgumentConfig();
        argument.setCallback(true);
        assertThat(argument.isCallback(), is(true));
    }

    @Test
    void testArguments() {
        ArgumentConfig argument = new ArgumentConfig();
        argument.setIndex(1);
        argument.setType("int");
        argument.setCallback(true);
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractServiceConfig.appendParameters(parameters, argument);
        assertThat(parameters, hasEntry("callback", "true"));
        assertThat(parameters.size(), is(1));
    }
}
