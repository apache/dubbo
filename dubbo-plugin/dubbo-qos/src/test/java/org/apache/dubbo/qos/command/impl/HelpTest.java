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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

class HelpTest {
    @Test
    void testMainHelp() {
        Help help = new Help(FrameworkModel.defaultModel());
        String output = help.execute(Mockito.mock(CommandContext.class), null);
        assertThat(output, containsString("greeting"));
        assertThat(output, containsString("help"));
        assertThat(output, containsString("ls"));
        assertThat(output, containsString("online"));
        assertThat(output, containsString("offline"));
        assertThat(output, containsString("quit"));
    }

    @Test
    void testGreeting() {
        Help help = new Help(FrameworkModel.defaultModel());
        String output = help.execute(Mockito.mock(CommandContext.class), new String[]{"greeting"});
        assertThat(output, containsString("COMMAND NAME"));
        assertThat(output, containsString("greeting"));
        assertThat(output, containsString("EXAMPLE"));
        assertThat(output, containsString("greeting dubbo"));
    }
}
