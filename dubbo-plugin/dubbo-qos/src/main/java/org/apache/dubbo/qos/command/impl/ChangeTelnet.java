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

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.Cmd;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

@Cmd(name = "cd", summary = "Change default service.", example = {
    "cd [service]"
})
public class ChangeTelnet implements BaseCommand {

    public static final AttributeKey<String> SERVICE_KEY = AttributeKey.valueOf("telnet.service");

    private final DubboProtocol dubboProtocol;

    public ChangeTelnet(FrameworkModel frameworkModel) {
        this.dubboProtocol = DubboProtocol.getDubboProtocol(frameworkModel);
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        Channel channel = commandContext.getRemote();

        if (ArrayUtils.isEmpty(args)) {
            return "Please input service name, eg: \r\ncd XxxService\r\ncd com.xxx.XxxService";
        }
        String message = args[0];
        StringBuilder buf = new StringBuilder();
        if ("/".equals(message) || "..".equals(message)) {
            String service = channel.attr(SERVICE_KEY).getAndRemove();
            buf.append("Cancelled default service ").append(service).append('.');
        } else {
            boolean found = false;
            for (Exporter<?> exporter : dubboProtocol.getExporters()) {
                if (message.equals(exporter.getInvoker().getInterface().getSimpleName())
                    || message.equals(exporter.getInvoker().getInterface().getName())
                    || message.equals(exporter.getInvoker().getUrl().getPath())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                channel.attr(SERVICE_KEY).set(message);
                buf.append("Used the ").append(message).append(" as default.\r\nYou can cancel default service by command: cd /");
            } else {
                buf.append("No such service ").append(message);
            }
        }
        return buf.toString();
    }
}
