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
package com.alibaba.dubbo.rpc.protocol.dubbo.telnet;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.utils.CollectionUtils;
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
import com.alibaba.fastjson.JSON;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * InvokeTelnetHandler
 */
@Activate
@Help(parameter = "[service.]method(args)", summary = "Invoke the service method.", detail = "Invoke the service method.")
public class InvokeTelnetHandler implements TelnetHandler {
    static final String INVOKE_MESSAGE_KEY = "telnet.invoke.method.message";

    static final String INVOKE_METHOD_LIST_KEY = "telnet.invoke.method.list";


    @Override
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
        if (i < 0 || !message.endsWith(")")) {
            return "Invalid parameters, format: service.method(args)";
        }
        String method = message.substring(0, i).trim();
        String args = message.substring(i + 1, message.length() - 1).trim();
        i = method.lastIndexOf(".");
        if (i >= 0) {
            service = method.substring(0, i).trim();
            method = method.substring(i + 1).trim();
        }
        List<Object> list;
        try {
            list = JSON.parseArray("[" + args + "]", Object.class);
        } catch (Throwable t) {
            return "Invalid json argument, cause: " + t.getMessage();
        }
        Invoker<?> invoker = null;
        Method invokeMethod = null;
        Collection<Exporter<?>> exporters = DubboProtocol.getDubboProtocol().getExporters();
        if (isInvokedSelectCommand(channel)) {
            invokeMethod = (Method) channel.getAttribute(SelectTelnetHandler.SELECT_METHOD_KEY);
            for (Exporter<?> exporter : exporters) {
                if (invokeMethod.getDeclaringClass().getName().equals(exporter.getInvoker().getInterface().getName())) {
                    invoker = exporter.getInvoker();
                    break;
                }
            }
        } else {
            if ((StringUtils.isBlank(service))) {
                if (exporters.size() != 1) {
                    //no default service we should not continue
                    return "Failed to find service !";
                }
            }
            for (Exporter<?> exporter : exporters) {
                if (StringUtils.isBlank(service)
                        || service.equals(exporter.getInvoker().getInterface().getSimpleName())
                        || service.equals(exporter.getInvoker().getInterface().getName())
                        || service.equals(exporter.getInvoker().getUrl().getPath())) {
                    invoker = exporter.getInvoker();
                    List<Method> methodList = findSameSignatureMethod(exporter.getInvoker().getInterface(), method, list);
                    if (CollectionUtils.isNotEmpty(methodList)) {
                        if (methodList.size() == 1) {
                            invokeMethod = methodList.get(0);
                        } else {
                            List<Method> matchMethods = findMatchMethods(methodList, list);
                            if (CollectionUtils.isNotEmpty(matchMethods)) {
                                if (matchMethods.size() == 1) {
                                    invokeMethod = matchMethods.get(0);
                                } else { //exist overridden method
                                    channel.setAttribute(INVOKE_METHOD_LIST_KEY, matchMethods);
                                    channel.setAttribute(INVOKE_MESSAGE_KEY, message);
                                    printSelectMessage(buf, matchMethods);
                                    return buf.toString();
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }

        if (invoker != null) {
            if (invokeMethod != null) {
                try {
                    Object[] array = PojoUtils.realize(list.toArray(), invokeMethod.getParameterTypes(), invokeMethod.getGenericParameterTypes());
                    RpcContext.getContext().setLocalAddress(channel.getLocalAddress()).setRemoteAddress(channel.getRemoteAddress());
                    long start = System.currentTimeMillis();
                    Object result = invoker.invoke(new RpcInvocation(invokeMethod, array)).recreate();
                    long end = System.currentTimeMillis();
                    buf.append(JSON.toJSONString(result));
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

    private List<Method> findSameSignatureMethod(Class clazz, String lookupMethodName, List<Object> args) {
        List<Method> sameSignatureMethods = new ArrayList<Method>();
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getName().equals(lookupMethodName) && method.getParameterTypes().length == args.size()) {
                sameSignatureMethods.add(method);
            }
        }
        return sameSignatureMethods;
    }

    private List<Method> findMatchMethods(List<Method> methods, List<Object> args) {
        List<Method> matchMethod = new ArrayList<Method>();
        for (Method method : methods) {
            if (isMatch(method, args)) {
                matchMethod.add(method);
            }
        }
        return matchMethod;
    }

    private static boolean isMatch(Method method, List<Object> args) {
        Class<?>[] types = method.getParameterTypes();
        if (types.length != args.size()) {
            return false;
        }
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            Object arg = args.get(i);

            if (arg == null) {
                if (type.isPrimitive()) {
                    return false;
                }

                // if the type is not primitive, we choose to believe what the invoker want is a null value
                continue;
            }

            if (ReflectUtils.isPrimitive(arg.getClass())) {
                // allow string arg to enum type, @see PojoUtils.realize0()
                if (arg instanceof String && type.isEnum()) {
                    continue;
                }

                if (!ReflectUtils.isPrimitive(type)) {
                    return false;
                }

                if (!ReflectUtils.isCompatible(type, arg)) {
                    return false;
                }
            } else if (arg instanceof Map) {
                String name = (String) ((Map<?, ?>) arg).get("class");
                if (StringUtils.isNotEmpty(name)) {
                    Class<?> cls = ReflectUtils.forName(name);
                    if (!type.isAssignableFrom(cls)) {
                        return false;
                    }
                } else {
                    return true;
                }
            } else if (arg instanceof Collection) {
                if (!type.isArray() && !type.isAssignableFrom(arg.getClass())) {
                    return false;
                }
            } else {
                if (!type.isAssignableFrom(arg.getClass())) {
                    return false;
                }
            }
        }
        return true;
    }


    private void printSelectMessage(StringBuilder buf, List<Method> methods) {
        buf.append("Methods:\r\n");
        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            buf.append(i + 1).append(". ").append(method.getName()).append("(");
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int n = 0; n < parameterTypes.length; n++) {
                buf.append(parameterTypes[n].getSimpleName());
                if (n != parameterTypes.length - 1) {
                    buf.append(",");
                }
            }
            buf.append(")\r\n");
        }
        buf.append("Please use the select command to select the method you want to invoke. eg: select 1");
    }

    private boolean isInvokedSelectCommand(Channel channel) {
        if (channel.hasAttribute(SelectTelnetHandler.SELECT_KEY)) {
            channel.removeAttribute(SelectTelnetHandler.SELECT_KEY);
            return true;
        }
        return false;
    }
}
