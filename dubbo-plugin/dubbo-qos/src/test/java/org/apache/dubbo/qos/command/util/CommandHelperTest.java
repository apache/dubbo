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
import org.apache.dubbo.qos.command.impl.ChangeTelnet;
import org.apache.dubbo.qos.command.impl.CountTelnet;
import org.apache.dubbo.qos.command.impl.DisableDetailProfiler;
import org.apache.dubbo.qos.command.impl.DisableRouterSnapshot;
import org.apache.dubbo.qos.command.impl.DisableSimpleProfiler;
import org.apache.dubbo.qos.command.impl.EnableDetailProfiler;
import org.apache.dubbo.qos.command.impl.EnableRouterSnapshot;
import org.apache.dubbo.qos.command.impl.EnableSimpleProfiler;
import org.apache.dubbo.qos.command.impl.GetAddress;
import org.apache.dubbo.qos.command.impl.GetConfig;
import org.apache.dubbo.qos.command.impl.GetEnabledRouterSnapshot;
import org.apache.dubbo.qos.command.impl.GetRecentRouterSnapshot;
import org.apache.dubbo.qos.command.impl.GetRouterSnapshot;
import org.apache.dubbo.qos.command.impl.Help;
import org.apache.dubbo.qos.command.impl.InvokeTelnet;
import org.apache.dubbo.qos.command.impl.Live;
import org.apache.dubbo.qos.command.impl.LoggerInfo;
import org.apache.dubbo.qos.command.impl.Ls;
import org.apache.dubbo.qos.command.impl.Offline;
import org.apache.dubbo.qos.command.impl.OfflineApp;
import org.apache.dubbo.qos.command.impl.OfflineInterface;
import org.apache.dubbo.qos.command.impl.Online;
import org.apache.dubbo.qos.command.impl.OnlineApp;
import org.apache.dubbo.qos.command.impl.OnlineInterface;
import org.apache.dubbo.qos.command.impl.PortTelnet;
import org.apache.dubbo.qos.command.impl.PublishMetadata;
import org.apache.dubbo.qos.command.impl.PwdTelnet;
import org.apache.dubbo.qos.command.impl.Quit;
import org.apache.dubbo.qos.command.impl.Ready;
import org.apache.dubbo.qos.command.impl.SelectTelnet;
import org.apache.dubbo.qos.command.impl.SerializeCheckStatus;
import org.apache.dubbo.qos.command.impl.SerializeWarnedClasses;
import org.apache.dubbo.qos.command.impl.SetProfilerWarnPercent;
import org.apache.dubbo.qos.command.impl.ShutdownTelnet;
import org.apache.dubbo.qos.command.impl.Startup;
import org.apache.dubbo.qos.command.impl.SwitchLogLevel;
import org.apache.dubbo.qos.command.impl.SwitchLogger;
import org.apache.dubbo.qos.command.impl.Version;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHelperTest {
    private CommandHelper commandHelper = new CommandHelper(FrameworkModel.defaultModel());

    @Test
    void testHasCommand() {
        assertTrue(commandHelper.hasCommand("greeting"));
        assertFalse(commandHelper.hasCommand("not-exiting"));
    }

    @Test
    void testGetAllCommandClass() {
        List<Class<?>> classes = commandHelper.getAllCommandClass();

        // update this list when introduce a new command
        List<Class<?>> expectedClasses = new LinkedList<>();
        expectedClasses.add(GreetingCommand.class);
        expectedClasses.add(Help.class);
        expectedClasses.add(Live.class);
        expectedClasses.add(Ls.class);
        expectedClasses.add(Offline.class);
        expectedClasses.add(OfflineApp.class);
        expectedClasses.add(OfflineInterface.class);
        expectedClasses.add(Online.class);
        expectedClasses.add(OnlineApp.class);
        expectedClasses.add(OnlineInterface.class);
        expectedClasses.add(PublishMetadata.class);
        expectedClasses.add(Quit.class);
        expectedClasses.add(Ready.class);
        expectedClasses.add(Startup.class);
        expectedClasses.add(Version.class);
        expectedClasses.add(ChangeTelnet.class);
        expectedClasses.add(CountTelnet.class);
        expectedClasses.add(InvokeTelnet.class);
        expectedClasses.add(SelectTelnet.class);
        expectedClasses.add(PortTelnet.class);
        expectedClasses.add(PwdTelnet.class);
        expectedClasses.add(ShutdownTelnet.class);
        expectedClasses.add(EnableDetailProfiler.class);
        expectedClasses.add(DisableDetailProfiler.class);
        expectedClasses.add(EnableSimpleProfiler.class);
        expectedClasses.add(DisableSimpleProfiler.class);
        expectedClasses.add(SetProfilerWarnPercent.class);
        expectedClasses.add(GetRouterSnapshot.class);
        expectedClasses.add(GetEnabledRouterSnapshot.class);
        expectedClasses.add(EnableRouterSnapshot.class);
        expectedClasses.add(DisableRouterSnapshot.class);
        expectedClasses.add(GetRecentRouterSnapshot.class);
        expectedClasses.add(LoggerInfo.class);
        expectedClasses.add(SwitchLogger.class);
        expectedClasses.add(SwitchLogLevel.class);
        expectedClasses.add(SerializeCheckStatus.class);
        expectedClasses.add(SerializeWarnedClasses.class);
        expectedClasses.add(GetConfig.class);
        expectedClasses.add(GetAddress.class);
        assertThat(classes, containsInAnyOrder(expectedClasses.toArray(new Class<?>[0])));
    }

    @Test
    void testGetCommandClass() {
        assertThat(commandHelper.getCommandClass("greeting"), equalTo(GreetingCommand.class));
        assertNull(commandHelper.getCommandClass("not-exiting"));
    }
}
