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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.Cmd;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.telnet.support.TelnetUtils;
import org.apache.dubbo.remoting.utils.PayloadDropper;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.qos.server.handler.QosProcessHandler.PROMPT;

@Cmd(name = "count", summary = "Count the service.", example = {
    "count [service] [method] [times]"
})
public class CountTelnet implements BaseCommand {
    private final DubboProtocol dubboProtocol;

    public CountTelnet(FrameworkModel frameworkModel) {
        this.dubboProtocol = DubboProtocol.getDubboProtocol(frameworkModel);
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        Channel channel = commandContext.getRemote();
        String service = channel.attr(ChangeTelnet.SERVICE_KEY).get();
        if ((service == null || service.length() == 0)
            && (args == null || args.length == 0)) {
            return "Please input service name, eg: \r\ncount XxxService\r\ncount XxxService xxxMethod\r\ncount XxxService xxxMethod 10\r\nor \"cd XxxService\" firstly.";
        }
        StringBuilder buf = new StringBuilder();
        if (service != null && service.length() > 0) {
            buf.append("Use default service ").append(service).append(".\r\n");
        }
        String method;
        String times;
        if (service == null || service.length() == 0) {
            service = args[0];
            method = args.length > 1 ? args[1] : null;
        } else {
            method = args.length > 0 ? args[0] : null;
        }
        if (StringUtils.isNumber(method)) {
            times = method;
            method = null;
        } else {
            times = args.length > 2 ? args[2] : "1";
        }
        if (!StringUtils.isNumber(times)) {
            return "Illegal times " + times + ", must be integer.";
        }
        final int t = Integer.parseInt(times);
        Invoker<?> invoker = null;
        for (Exporter<?> exporter : dubboProtocol.getExporters()) {
            if (service.equals(exporter.getInvoker().getInterface().getSimpleName())
                || service.equals(exporter.getInvoker().getInterface().getName())
                || service.equals(exporter.getInvoker().getUrl().getPath())) {
                invoker = exporter.getInvoker();
                break;
            }
        }
        if (invoker != null) {
            if (t > 0) {
                final String mtd = method;
                final Invoker<?> inv = invoker;
                Thread thread = new Thread(() -> {
                    for (int i = 0; i < t; i++) {
                        String result = count(inv, mtd);
                        try {
                            send(channel, "\r\n" + result);
                        } catch (RemotingException e1) {
                            return;
                        }
                        if (i < t - 1) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored) {
                            }
                        }
                    }
                    try {
                        send(channel, "\r\n" + PROMPT);
                    } catch (RemotingException ignored) {
                    }
                }, "TelnetCount");
                thread.setDaemon(true);
                thread.start();
            }
        } else {
            buf.append("No such service ").append(service);
        }
        return buf.toString();
    }

    public void send(Channel channel, Object message) throws RemotingException {
        boolean success;
        int timeout = 0;
        try {
            ChannelFuture future = channel.writeAndFlush(message);
            success = future.await(DEFAULT_TIMEOUT);
            Throwable cause = future.cause();
            if (cause != null) {
                throw cause;
            }
        } catch (Throwable e) {
            throw new RemotingException((InetSocketAddress) channel.localAddress(), (InetSocketAddress) channel.remoteAddress(), "Failed to send message " + PayloadDropper.getRequestWithoutData(message) + " to " + channel.remoteAddress().toString() + ", cause: " + e.getMessage(), e);
        }
        if (!success) {
            throw new RemotingException((InetSocketAddress) channel.localAddress(), (InetSocketAddress) channel.remoteAddress(), "Failed to send message " + PayloadDropper.getRequestWithoutData(message) + " to " + channel.remoteAddress().toString()
                + "in timeout(" + timeout + "ms) limit");
        }
    }

    private String count(Invoker<?> invoker, String method) {
        URL url = invoker.getUrl();
        List<List<String>> table = new ArrayList<List<String>>();
        List<String> header = new ArrayList<String>();
        header.add("method");
        header.add("total");
        header.add("failed");
        header.add("active");
        header.add("average");
        header.add("max");
        if (method == null || method.length() == 0) {
            for (Method m : invoker.getInterface().getMethods()) {
                RpcStatus count = RpcStatus.getStatus(url, m.getName());
                table.add(createRow(m.getName(), count));
            }
        } else {
            boolean found = false;
            for (Method m : invoker.getInterface().getMethods()) {
                if (m.getName().equals(method)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                RpcStatus count = RpcStatus.getStatus(url, method);
                table.add(createRow(method, count));
            } else {
                return "No such method " + method + " in class " + invoker.getInterface().getName();
            }
        }
        return TelnetUtils.toTable(header, table);
    }

    private List<String> createRow(String methodName, RpcStatus count) {
        List<String> row = new ArrayList<String>();
        row.add(methodName);
        row.add(String.valueOf(count.getTotal()));
        row.add(String.valueOf(count.getFailed()));
        row.add(String.valueOf(count.getActive()));
        row.add(count.getSucceededAverageElapsed() + "ms");
        row.add(count.getSucceededMaxElapsed() + "ms");
        return row;
    }
}
