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
package org.apache.dubbo.qos.legacy;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.remoting.telnet.support.Help;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;
import org.apache.dubbo.rpc.protocol.dubbo.filter.TraceFilter;

import java.lang.reflect.Method;

/**
 * TraceTelnetHandler
 */
@Activate
@Help(parameter = "[service] [method] [times]", summary = "Trace the service.", detail = "Trace the service.")
public class TraceTelnetHandler implements TelnetHandler {

    @Override
    public String telnet(Channel channel, String message) {
        String service = (String) channel.getAttribute(ChangeTelnetHandler.SERVICE_KEY);
        if ((StringUtils.isEmpty(service))
                && (StringUtils.isEmpty(message))) {
            return "Please input service name, eg: \r\ntrace XxxService\r\ntrace XxxService xxxMethod\r\ntrace XxxService xxxMethod 10\r\nor \"cd XxxService\" firstly.";
        }
        String[] parts = message.split("\\s+");
        String method;
        String times;
        // message like : XxxService , XxxService 10 , XxxService xxxMethod , XxxService xxxMethod 10
        if (StringUtils.isEmpty(service)) {
            service = parts.length > 0 ? parts[0] : null;
            method = parts.length > 1 ? parts[1] : null;
            times = parts.length > 2 ? parts[2] : "1";
        } else {  //message like : xxxMethod, xxxMethod 10
            method = parts.length > 0 ? parts[0] : null;
            times = parts.length > 1 ? parts[1] : "1";
        }
        if (StringUtils.isNumber(method)) {
            times = method;
            method = null;
        }
        if (!StringUtils.isNumber(times)) {
            return "Illegal times " + times + ", must be integer.";
        }
        Invoker<?> invoker = null;
        for (Exporter<?> exporter : DubboProtocol.getDubboProtocol().getExporters()) {
            if (service.equals(exporter.getInvoker().getInterface().getSimpleName())
                    || service.equals(exporter.getInvoker().getInterface().getName())
                    || service.equals(exporter.getInvoker().getUrl().getPath())) {
                invoker = exporter.getInvoker();
                break;
            }
        }
        if (invoker != null) {
            if (StringUtils.isNotEmpty(method)) {
                boolean found = false;
                for (Method m : invoker.getInterface().getMethods()) {
                    if (m.getName().equals(method)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return "No such method " + method + " in class " + invoker.getInterface().getName();
                }
            }
            TraceFilter.addTracer(invoker.getInterface(), method, channel, Integer.parseInt(times));
        } else {
            return "No such service " + service;
        }
        return null;
    }

}
