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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.Cmd;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.utils.PojoUtils.realize;

@Cmd(name = "invoke", summary = "Invoke the service method.", example = {
    "invoke IHelloService.sayHello(\"xxxx\")",
    "invoke sayHello(\"xxxx\")"
})
public class InvokeTelnet implements BaseCommand {
    public static final AttributeKey<String> INVOKE_MESSAGE_KEY = AttributeKey.valueOf("telnet.invoke.method.message");
    public static final AttributeKey<List<Method>> INVOKE_METHOD_LIST_KEY = AttributeKey.valueOf("telnet.invoke.method.list");
    public static final AttributeKey<ProviderModel> INVOKE_METHOD_PROVIDER_KEY = AttributeKey.valueOf("telnet.invoke.method.provider");

    private final FrameworkModel frameworkModel;

    public InvokeTelnet(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        if (ArrayUtils.isEmpty(args)) {
            return "Please input method name, eg: \r\ninvoke xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\n" +
                "invoke XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})\r\n" +
                "invoke com.xxx.XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})";
        }
        Channel channel = commandContext.getRemote();
        String service = channel.attr(ChangeTelnet.SERVICE_KEY) != null ? channel.attr(ChangeTelnet.SERVICE_KEY).get() : null;

        String message = args[0];
        int i = message.indexOf("(");

        if (i < 0 || !message.endsWith(")")) {
            return "Invalid parameters, format: service.method(args)";
        }

        String method = message.substring(0, i).trim();
        String param = message.substring(i + 1, message.length() - 1).trim();
        i = method.lastIndexOf(".");
        if (i >= 0) {
            service = method.substring(0, i).trim();
            method = method.substring(i + 1).trim();
        }

        if (StringUtils.isEmpty(service)) {
            return "If you want to invoke like [invoke sayHello(\"xxxx\")], please execute cd command first," +
                " or you can execute it like [invoke IHelloService.sayHello(\"xxxx\")]";
        }

        List<Object> list;
        try {
            list = JsonUtils.toJavaList("[" + param + "]", Object.class);
        } catch (Throwable t) {
            return "Invalid json argument, cause: " + t.getMessage();
        }
        StringBuilder buf = new StringBuilder();
        Method invokeMethod = null;
        ProviderModel selectedProvider = null;
        if (isInvokedSelectCommand(channel)) {
            selectedProvider = channel.attr(INVOKE_METHOD_PROVIDER_KEY).get();
            invokeMethod = channel.attr(SelectTelnet.SELECT_METHOD_KEY).get();
        } else {
            for (ProviderModel provider : frameworkModel.getServiceRepository().allProviderModels()) {
                if (!isServiceMatch(service, provider)) {
                    continue;
                }

                selectedProvider = provider;
                List<Method> methodList = findSameSignatureMethod(provider.getAllMethods(), method, list);
                if (CollectionUtils.isEmpty(methodList)) {
                    break;
                }

                if (methodList.size() == 1) {
                    invokeMethod = methodList.get(0);
                } else {
                    List<Method> matchMethods = findMatchMethods(methodList, list);
                    if (CollectionUtils.isEmpty(matchMethods)) {
                        break;
                    }
                    if (matchMethods.size() == 1) {
                        invokeMethod = matchMethods.get(0);
                    } else { //exist overridden method
                        channel.attr(INVOKE_METHOD_PROVIDER_KEY).set(provider);
                        channel.attr(INVOKE_METHOD_LIST_KEY).set(matchMethods);
                        channel.attr(INVOKE_MESSAGE_KEY).set(message);
                        printSelectMessage(buf, matchMethods);
                        return buf.toString();
                    }
                }
                break;
            }
        }


        if (!StringUtils.isEmpty(service)) {
            buf.append("Use default service ").append(service).append('.');
        }
        if (selectedProvider == null) {
            buf.append("\r\nNo such service ").append(service);
            return buf.toString();
        }
        if (invokeMethod == null) {
            buf.append("\r\nNo such method ").append(method).append(" in service ").append(service);
            return buf.toString();
        }
        try {
            Object[] array = realize(list.toArray(), invokeMethod.getParameterTypes(),
                invokeMethod.getGenericParameterTypes());
            long start = System.currentTimeMillis();
            AppResponse result = new AppResponse();
            try {
                Object o = invokeMethod.invoke(selectedProvider.getServiceInstance(), array);
                result.setValue(o);
            } catch (Throwable t) {
                result.setException(t);
            }
            long end = System.currentTimeMillis();
            buf.append("\r\nresult: ");
            buf.append(JsonUtils.toJson(result.recreate()));
            buf.append("\r\nelapsed: ");
            buf.append(end - start);
            buf.append(" ms.");
        } catch (Throwable t) {
            return "Failed to invoke method " + invokeMethod.getName() + ", cause: " + StringUtils.toString(t);
        }
        return buf.toString();
    }


    private boolean isServiceMatch(String service, ProviderModel provider) {
        return provider.getServiceKey().equalsIgnoreCase(service)
            || provider.getServiceInterfaceClass().getSimpleName().equalsIgnoreCase(service)
            || provider.getServiceInterfaceClass().getName().equalsIgnoreCase(service)
            || StringUtils.isEmpty(service);
    }

    private List<Method> findSameSignatureMethod(Set<MethodDescriptor> methods, String lookupMethodName, List<Object> args) {
        List<Method> sameSignatureMethods = new ArrayList<>();
        for (MethodDescriptor model : methods) {
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
            buf.append(i + 1).append(". ").append(method.getName()).append('(');
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int n = 0; n < parameterTypes.length; n++) {
                buf.append(parameterTypes[n].getSimpleName());
                if (n != parameterTypes.length - 1) {
                    buf.append(',');
                }
            }
            buf.append(")\r\n");
        }
        buf.append("Please use the select command to select the method you want to invoke. eg: select 1");
    }

    private boolean isInvokedSelectCommand(Channel channel) {
        if (channel.attr(SelectTelnet.SELECT_KEY).get() != null) {
            channel.attr(SelectTelnet.SELECT_KEY).remove();
            return true;
        }
        return false;
    }
}
