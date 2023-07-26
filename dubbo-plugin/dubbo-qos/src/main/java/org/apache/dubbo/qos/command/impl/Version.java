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

import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.Cmd;

@Cmd(name = "version", summary = "version command(show dubbo version)", example = {
        "version"
})
public class Version implements BaseCommand {

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        StringBuilder versionDescBuilder = new StringBuilder();
        versionDescBuilder.append("dubbo version \"");
        versionDescBuilder.append(org.apache.dubbo.common.Version.getVersion());
        versionDescBuilder.append('\"');
        return versionDescBuilder.toString();
    }

}
