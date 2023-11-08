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
package org.apache.dubbo.maven.plugin.protoc;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.util.Os;

public class DubboProtocPluginWrapperFactory {

    private final LinuxDubboProtocPluginWrapper linuxProtocCommandBuilder = new LinuxDubboProtocPluginWrapper();
    private final WinDubboProtocPluginWrapper winDubboProtocPluginWrapper = new WinDubboProtocPluginWrapper();

    private final Map<String, DubboProtocPluginWrapper> dubboProtocPluginWrappers = new HashMap<>();

    public DubboProtocPluginWrapperFactory() {
        dubboProtocPluginWrappers.put("linux", linuxProtocCommandBuilder);
        dubboProtocPluginWrappers.put("windows", winDubboProtocPluginWrapper);
    }

    public DubboProtocPluginWrapper findByOs() {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return dubboProtocPluginWrappers.get("windows");
        }
        return dubboProtocPluginWrappers.get("linux");
    }
}
