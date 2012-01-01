/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.dubbo.telnet;

import java.lang.reflect.Method;
import java.util.List;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.json.JSON;
import com.alibaba.dubbo.common.utils.PojoUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.remoting.telnet.support.Help;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;

/**
 * InvokeTelnetHandler
 * 
 * @author william.liangf
 */
@Help(parameter = "[service.]method(args)", summary = "Invoke the service method.", detail = "Invoke the service method.")
@Extension("invoke")
public class InvokeTelnetHandler implements TelnetHandler {

    @SuppressWarnings("unchecked")
    public String telnet(Channel channel, String message) {
        if (message == null || message.length() == 0) {
            return "Please input method name, eg: \r\ninvoke xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\ninvoke com.xxx.XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})";
        }
        StringBuilder buf = new StringBuilder();
        String service = (String) channel.getAttribute(ChangeTelnetHandler.SERVICE_KEY);
        if (service != null && service.length() > 0) {
            buf.append("Use default service " + service + ".\r\n");
        }
        int i = message.indexOf("(");
        if (i < 0 || ! message.endsWith(")")) {
            return "Invalid parameters, format: service.method(args)";
        }
        String method = message.substring(0, i).trim();
        String args = message.substring(i + 1, message.length() - 1).trim();
        i = method.lastIndexOf(".");
        if (i >= 0) {
            service = method.substring(0, i).trim();
            method = method.substring(i + 1).trim();
        }
        Invoker<?> invoker = null;
        Method invokeMethod = null;
        for (Exporter<?> exporter : DubboProtocol.getDubboProtocol().getExporters()) {
            if (service == null || service.length() == 0) {
                Method[] methods = exporter.getInvoker().getInterface().getMethods();
                for (Method m : methods) {
                    if (m.getName().equals(method)
                            || ReflectUtils.getSignature(m.getName(), m.getParameterTypes()).equals(method)) {
                        invoker = exporter.getInvoker();
                        invokeMethod = m;
                        break;
                    }
                }
                if (invoker != null) {
                    break;
                }
            } else {
                if (service.equals(exporter.getInvoker().getInterface().getSimpleName())
                        || service.equals(exporter.getInvoker().getInterface().getName())
                        || service.equals(exporter.getInvoker().getUrl().getPath())) {
                    invoker = exporter.getInvoker();
                    Method[] methods = invoker.getInterface().getMethods();
                    for (Method m : methods) {
                        if (m.getName().equals(method)
                                || ReflectUtils.getSignature(m.getName(), m.getParameterTypes()).equals(method)) {
                            invokeMethod = m;
                        }
                    }
                    break;
                }
            }
        }
        if (invoker != null) {
            if (invokeMethod != null) {
                List<Object> list;
                try {
                    list = (List<Object>) JSON.parse("[" + args + "]", List.class);
                } catch (Throwable t) {
                    return "Invalid json argument, cause: " + t.getMessage();
                }
                try {
                    Object[] array = PojoUtils.realize(list.toArray(), invokeMethod.getParameterTypes());
                    RpcContext.getContext().setLocalAddress(channel.getLocalAddress()).setRemoteAddress(channel.getRemoteAddress());
                    long start = System.currentTimeMillis();
                    Object result = invoker.invoke(new RpcInvocation(invokeMethod, array)).recreate();
                    long end = System.currentTimeMillis();
                    buf.append(JSON.json(result));
                    buf.append("\r\nelapsed: ");
                    buf.append(end - start);
                    buf.append(" ms.");
                } catch (Throwable t) {
                    return "Failed to invoke method " + invokeMethod.getName() + ", cause: " + StringUtils.toString(t);
                }
            } else {
                buf.append("No such method " + method + " in service " + service);
            }
        } else {
            buf.append("No such service " + service);
        }
        return buf.toString();
    }

}