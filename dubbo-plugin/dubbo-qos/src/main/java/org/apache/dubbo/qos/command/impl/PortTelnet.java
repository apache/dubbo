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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;

import java.util.Collection;

@Cmd(name = "ps", summary = "Print server ports and connections.", example = {
    "ps -l [port]", "ps", "ps -l", "ps -l 20880"
})
public class PortTelnet implements BaseCommand {
    private DubboProtocol dubboProtocol;

    public PortTelnet(FrameworkModel frameworkModel) {
        this.dubboProtocol = DubboProtocol.getDubboProtocol(frameworkModel);
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        StringBuilder buf = new StringBuilder();
        String port = null;
        boolean detail = false;
        if (args.length > 0) {
            for (String part : args) {
                if ("-l".equals(part)) {
                    detail = true;
                } else {
                    if (!StringUtils.isInteger(part)) {
                        return "Illegal port " + part + ", must be integer.";
                    }
                    port = part;
                }
            }
        }
        if (StringUtils.isEmpty(port)) {
            for (ProtocolServer server : dubboProtocol.getServers()) {
                if (buf.length() > 0) {
                    buf.append("\r\n");
                }
                if (detail) {
                    buf.append(server.getUrl().getProtocol()).append("://").append(server.getUrl().getAddress());
                } else {
                    buf.append(server.getUrl().getPort());
                }
            }
        } else {
            int p = Integer.parseInt(port);
            ProtocolServer protocolServer = null;
            for (ProtocolServer s : dubboProtocol.getServers()) {
                if (p == s.getUrl().getPort()) {
                    protocolServer = s;
                    break;
                }
            }
            if (protocolServer != null) {
                ExchangeServer server = (ExchangeServer) protocolServer.getRemotingServer();
                Collection<ExchangeChannel> channels = server.getExchangeChannels();
                for (ExchangeChannel c : channels) {
                    if (buf.length() > 0) {
                        buf.append("\r\n");
                    }
                    if (detail) {
                        buf.append(c.getRemoteAddress()).append(" -> ").append(c.getLocalAddress());
                    } else {
                        buf.append(c.getRemoteAddress());
                    }
                }
            } else {
                buf.append("No such port ").append(port);
            }
        }
        return buf.toString();
    }
}
