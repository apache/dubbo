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
package org.apache.dubbo.rpc.protocol.dubbo.telnet;

import com.alibaba.fastjson.JSON;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.remoting.telnet.support.Help;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderMethodModel;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.utils.PojoUtils.realize;

/**
 * InvokeTelnetHandler
 */
@Activate
@Help(parameter = "[service.]method(args) ", summary = "Invoke the service method.",
        detail = "Invoke the service method.")
public class InvokeTelnetHandler implements TelnetHandler {

    public static final String INVOKE_MESSAGE_KEY = "telnet.invoke.method.message";
    public static final String INVOKE_METHOD_LIST_KEY = "telnet.invoke.method.list";
    public static final String INVOKE_METHOD_PROVIDER_KEY = "telnet.invoke.method.provider";

    @Override
    @SuppressWarnings("unchecked")
    public String telnet(Channel channel, String message) {
        if (StringUtils.isEmpty(message)) {
            return "Please input method name, eg: \r\ninvoke xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\n" +
                    "invoke XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\n" +
                    "invoke com.xxx.XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})";
        }

        String service = (String) channel.getAttribute(ChangeTelnetHandler.SERVICE_KEY);

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
        StringBuilder buf = new StringBuilder();
        Method invokeMethod = null;
        ProviderModel selectedProvider = null;
        if (isInvokedSelectCommand(channel)) {
            selectedProvider = (ProviderModel) channel.getAttribute(INVOKE_METHOD_PROVIDER_KEY);
            invokeMethod = (Method) channel.getAttribute(SelectTelnetHandler.SELECT_METHOD_KEY);
        } else {
            for (ProviderModel provider : ApplicationModel.allProviderModels()) {
                if (isServiceMatch(service, provider)) {
                    selectedProvider = provider;
                    List<Method> methodList = findSameSignatureMethod(provider.getAllMethods(), method, list);
                    if (CollectionUtils.isNotEmpty(methodList)) {
                        if (methodList.size() == 1) {
                            invokeMethod = methodList.get(0);
                        } else {
                            List<Method> matchMethods = findMatchMethods(methodList, list);
                            if (CollectionUtils.isNotEmpty(matchMethods)) {
                                if (matchMethods.size() == 1) {
                                    invokeMethod = matchMethods.get(0);
                                } else { //exist overridden method
                                    channel.setAttribute(INVOKE_METHOD_PROVIDER_KEY, provider);
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


        if (!StringUtils.isEmpty(service)) {
            buf.append("Use default service ").append(service).append(".");
        }
        if (selectedProvider != null) {
            if (invokeMethod != null) {
                try {
                    Object[] array = realize(list.toArray(), invokeMethod.getParameterTypes(),
                            invokeMethod.getGenericParameterTypes());
                    long start = System.currentTimeMillis();
                    RpcResult result = new RpcResult();
                    try {
                        Object o = invokeMethod.invoke(selectedProvider.getServiceInstance(), array);
                        result.setValue(o);
                    } catch (Throwable t) {
                        result.setException(t);
                    }
                    long end = System.currentTimeMillis();
                    buf.append("\r\nresult: ");
                    buf.append(JSON.toJSONString(result.recreate()));
                    buf.append("\r\nelapsed: ");
                    buf.append(end - start);
                    buf.append(" ms.");
                } catch (Throwable t) {
                    return "Failed to invoke method " + invokeMethod.getName() + ", cause: " + StringUtils.toString(t);
                }
            } else {
                buf.append("\r\nNo such method ").append(method).append(" in service ").append(service);
            }
        } else {
            buf.append("\r\nNo such service ").append(service);
        }
        return buf.toString();
    }


    private boolean isServiceMatch(String service, ProviderModel provider) {
        return provider.getServiceName().equalsIgnoreCase(service)
                || provider.getServiceInterfaceClass().getSimpleName().equalsIgnoreCase(service)
                || provider.getServiceInterfaceClass().getName().equalsIgnoreCase(service)
                || StringUtils.isEmpty(service);
    }

    private List<Method> findSameSignatureMethod(List<ProviderMethodModel> methods, String lookupMethodName, List<Object> args) {
        List<Method> sameSignatureMethods = new ArrayList<>();
        for (ProviderMethodModel model : methods) {
            Method method = model.getMethod();
            if (method.getName().equals(lookupMethodName) && method.getParameterTypes().length == args.size()) {
                sameSignatureMethods.add(method);
            }
        }
        return sameSignatureMethods;
    }

    private List<Method> findMatchMethods(List<Method> methods, List<Object> args) {
        List<Method> matchMethod = new ArrayList<>();
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
            buf.append((i + 1) + ". " + method.getName() + "(");
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
