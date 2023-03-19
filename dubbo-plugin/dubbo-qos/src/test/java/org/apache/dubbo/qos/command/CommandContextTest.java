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

package org.apache.dubbo.qos.command;

import io.netty.channel.Channel;
import org.apache.dubbo.qos.api.CommandContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandContextTest {
    @Test
    void test() {
        CommandContext context = new CommandContext("test", new String[]{"hello"}, true);
        Object request = new Object();
        context.setOriginRequest(request);
        Channel channel = Mockito.mock(Channel.class);
        context.setRemote(channel);
        assertThat(context.getCommandName(), equalTo("test"));
        assertThat(context.getArgs(), arrayContaining("hello"));
        assertThat(context.getOriginRequest(), is(request));
        assertTrue(context.isHttp());
        assertThat(context.getRemote(), is(channel));

        context = new CommandContext("command");
        context.setRemote(channel);
        context.setOriginRequest(request);
        context.setArgs(new String[]{"world"});
        context.setHttp(false);
        assertThat(context.getCommandName(), equalTo("command"));
        assertThat(context.getArgs(), arrayContaining("world"));
        assertThat(context.getOriginRequest(), is(request));
        assertFalse(context.isHttp());
        assertThat(context.getRemote(), is(channel));
    }
}
