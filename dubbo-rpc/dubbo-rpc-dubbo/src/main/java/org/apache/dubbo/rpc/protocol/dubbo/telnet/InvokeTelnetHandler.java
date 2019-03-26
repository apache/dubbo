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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

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
    private static Method findMethod(List<ProviderMethodModel> methods, String method, List<Object> args,
                                     Class<?>[] paramTypes) {
        for (ProviderMethodModel model : methods) {
            Method m = model.getMethod();
            if (isMatch(m, args, paramTypes,method)) {
                return m;
            }
        }
        return null;
    }

    private static boolean isMatch(Method method,List<Object> args, Class<?>[] paramClasses,String lookupMethodName) {
        if(!method.getName().equals(lookupMethodName)) {
            return false;
        }

        Class<?> types[]=method.getParameterTypes();
        if (types.length != args.size()) {
            return false;
        }
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            Object arg = args.get(i);

            if (paramClasses != null && type != paramClasses[i]) {
                return false;
            }

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
                    return false;
                }

                if (!ReflectUtils.isCompatible(type, arg)) {
                    return false;
                }
            } else if (arg instanceof Map) {
                String name = (String) ((Map<?, ?>) arg).get("class");
                Class<?> cls = arg.getClass();
                if (name != null && name.length() > 0) {
                    cls = ReflectUtils.forName(name);
                }
                if (!type.isAssignableFrom(cls)) {
                    return false;
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
            buf.append("Use default service ").append(service).append(".\r\n");
        }

        int i = message.indexOf("(");
        String originalMessage = message;
        Class<?>[] paramTypes = null;
        if (message.contains("-p")) {
            message = originalMessage.substring(0, originalMessage.indexOf("-p")).trim();
            String paramClassesString = originalMessage.substring(originalMessage.indexOf("-p") + 2).trim();
            if (paramClassesString.length() > 0) {
                String[] split = paramClassesString.split("\\s+");
                if (split.length > 0) {
                    paramTypes = new Class[split.length];
                    for (int j = 0; j < split.length; j++) {
                        try {
                            paramTypes[j] = Class.forName(split[j]);
                        } catch (ClassNotFoundException e) {
                            return "Unknown parameter class for name " + split[j];
                        }
                    }

                }
            }
        }

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
        if (paramTypes != null) {
            if (paramTypes.length != list.size()) {
                return "Parameter's number does not match the number of parameter class";
            }
            List<Object> listOfActualClass = new ArrayList<>(list.size());
            for (int ii = 0; ii < list.size(); ii++) {
                if (list.get(ii) instanceof JSONObject) {
                    JSONObject jsonObject = (JSONObject) list.get(ii);
                    listOfActualClass.add(jsonObject.toJavaObject(paramTypes[ii]));
                } else {
                    listOfActualClass.add(list.get(ii));
                }
            }
            list = listOfActualClass;
        }

        Method invokeMethod = null;
        ProviderModel selectedProvider = null;
        for (ProviderModel provider : ApplicationModel.allProviderModels()) {
            if (isServiceMatch(service, provider)) {
                invokeMethod = findMethod(provider.getAllMethods(), method, list, paramTypes);
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
                    buf.append(JSON.toJSONString(result.recreate()));
                    buf.append("\r\nelapsed: ");
                    buf.append(end - start);
                    buf.append(" ms.");
                } catch (Throwable t) {
                    return "Failed to invoke method " + invokeMethod.getName() + ", cause: " + StringUtils.toString(t);
                }
            } else {
                buf.append("No such method ").append(method).append(" in service ").append(service);
            }
        } else {
            buf.append("No such service ").append(service);
        }
        return buf.toString();
    }

    private boolean isServiceMatch(String service, ProviderModel provider) {
        return provider.getServiceName().equalsIgnoreCase(service)
                || provider.getServiceInterfaceClass().getSimpleName().equalsIgnoreCase(service)
                || provider.getServiceInterfaceClass().getName().equalsIgnoreCase(service)
                || StringUtils.isEmpty(service);
    }
}
