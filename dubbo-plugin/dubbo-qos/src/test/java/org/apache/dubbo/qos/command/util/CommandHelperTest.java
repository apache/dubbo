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
package org.apache.dubbo.qos.command.util;

import org.apache.dubbo.qos.command.GreetingCommand;
import org.apache.dubbo.qos.command.impl.Help;
import org.apache.dubbo.qos.command.impl.Ls;
import org.apache.dubbo.qos.command.impl.Offline;
import org.apache.dubbo.qos.command.impl.Online;
import org.apache.dubbo.qos.command.impl.Quit;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandHelperTest {
    @Test
    public void testHasCommand() throws Exception {
        assertTrue(CommandHelper.hasCommand("greeting"));
        assertFalse(CommandHelper.hasCommand("not-exiting"));
    }

    @Test
    public void testGetAllCommandClass() throws Exception {
        List<Class<?>> classes = CommandHelper.getAllCommandClass();
        assertThat(classes, containsInAnyOrder(GreetingCommand.class, Help.class, Ls.class, Offline.class, Online.class, Quit.class));
    }

    @Test
    public void testGetCommandClass() throws Exception {
        assertThat(CommandHelper.getCommandClass("greeting"), equalTo(GreetingCommand.class));
        assertNull(CommandHelper.getCommandClass("not-exiting"));
    }
}
