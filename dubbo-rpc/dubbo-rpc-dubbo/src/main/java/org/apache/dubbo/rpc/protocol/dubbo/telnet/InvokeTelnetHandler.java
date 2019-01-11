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
import static org.apache.dubbo.rpc.RpcContext.getContext;

/**
 * InvokeTelnetHandler
 */
@Activate
@Help(parameter = "[service.]method(args) [-p parameter classes]", summary = "Invoke the service method.",
        detail = "Invoke the service method.")
public class InvokeTelnetHandler implements TelnetHandler {

    @Override
    @SuppressWarnings("unchecked")
    public String telnet(Channel channel, String message) {
        if (StringUtils.isEmpty(message)) {
            return "Please input method name, eg: \r\ninvoke xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\n" +
                    "invoke XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\n" +
                    "invoke com.xxx.XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})";
        }

        StringBuilder buf = new StringBuilder();
        String service = (String) channel.getAttribute(ChangeTelnetHandler.SERVICE_KEY);
        if (!StringUtils.isEmpty(service)) {
            buf.append("Use default service ").append(service).append(".");
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

        Method invokeMethod = null;
        ProviderModel selectedProvider = null;
        for (ProviderModel provider : ApplicationModel.allProviderModels()) {
            if (isServiceMatch(service, provider)) {
                List<Method> methodList = findSameSignatureMethod(provider.getAllMethods(), method, list);
                try {
                    invokeMethod = findInvokeMethod(methodList, list);
                } catch (Throwable t) {
                    return "Failed to invoke method " + method + ", cause: " + t.getMessage();
                }
                selectedProvider = provider;
                break;
            }
        }

        if (selectedProvider != null) {
            if (invokeMethod != null) {
                try {
                    Object[] array = realize(list.toArray(), invokeMethod.getParameterTypes(),
                            invokeMethod.getGenericParameterTypes());
                    getContext().setLocalAddress(channel.getLocalAddress()).setRemoteAddress(channel.getRemoteAddress());
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


    private Method findInvokeMethod(List<Method> methods, List<Object> args) {
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean allParameterMatch = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> type = parameterTypes[i];
                Object arg = args.get(i);

                if (arg == null) {
                    // if the type is primitive, the method to invoke will cause NullPointerException definitely
                    // so we can offer a specified error message to the invoker in advance and avoid unnecessary invoking
                    if (type.isPrimitive()) {
                        throw new NullPointerException(String.format("The type of No.%d parameter is primitive(%s), " +
                                "but the value passed is null.", i + 1, type.getName()));
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
                        allParameterMatch = false;
                        break;
                    }

                    if (!ReflectUtils.isCompatible(type, arg)) {
                        allParameterMatch = false;
                        break;
                    }
                } else if (arg instanceof Map) {
                    String name = (String) ((Map<?, ?>) arg).get("class");
                    if (StringUtils.isNotEmpty(name)) {
                        Class<?> cls = ReflectUtils.forName(name);
                        if (!type.isAssignableFrom(cls)) {
                            allParameterMatch = false;
                            break;
                        }
                    } else {
                        if (methods.size() == 1) {
                            continue;
                        }
                        throw new IllegalArgumentException("This method has same signature method," +
                                "No." + (i + 1) + " parameter must contains key \"class\"," +
                                "and it's value is the full name of this parameter. " +
                                "eg: invoke xxxMethod({\"prop\" : \"value\",\"class\":\"org.apache.dubbo.demo.DemoClass\"})"
                        );
                    }
                } else if (arg instanceof Collection) {
                    if (!type.isArray() && !type.isAssignableFrom(arg.getClass())) {
                        allParameterMatch = false;
                        break;
                    }
                } else {
                    if (!type.isAssignableFrom(arg.getClass())) {
                        allParameterMatch = false;
                        break;
                    }
                }
            }
            if (allParameterMatch) {
                return method;
            }
        }
        return null;
    }

}
