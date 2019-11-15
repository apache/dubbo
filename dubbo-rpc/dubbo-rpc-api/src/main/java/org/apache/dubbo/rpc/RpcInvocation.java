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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;

/**
 * RPC Invocation.
 *
 * @serial Don't change the class name and properties.
 */
public class RpcInvocation implements Invocation, Serializable {

    private static final long serialVersionUID = -4355285085441097045L;

    private String targetServiceUniqueName;

    private String methodName;
    private String serviceName;

    private transient Class<?>[] parameterTypes;
    private String parameterTypesDesc;
    private String[] compatibleParamSignatures;

    private Object[] arguments;

    private Map<String, Object> attachments;

    private Map<Object, Object> attributes = new HashMap<Object, Object>();

    private transient Invoker<?> invoker;

    private transient Class<?> returnType;

    private transient Type[] returnTypes;

    private transient InvokeMode invokeMode;

    public RpcInvocation() {
    }

    public RpcInvocation(Invocation invocation, Invoker<?> invoker) {
        this(invocation.getMethodName(), invocation.getServiceName(), invocation.getParameterTypes(),
                invocation.getArguments(), new HashMap<>(invocation.getAttachments()),
                invocation.getInvoker());
        if (invoker != null) {
            URL url = invoker.getUrl();
            setAttachment(PATH_KEY, url.getPath());
            if (url.hasParameter(INTERFACE_KEY)) {
                setAttachment(INTERFACE_KEY, url.getParameter(INTERFACE_KEY));
            }
            if (url.hasParameter(GROUP_KEY)) {
                setAttachment(GROUP_KEY, url.getParameter(GROUP_KEY));
            }
            if (url.hasParameter(VERSION_KEY)) {
                setAttachment(VERSION_KEY, url.getParameter(VERSION_KEY, "0.0.0"));
            }
            if (url.hasParameter(TIMEOUT_KEY)) {
                setAttachment(TIMEOUT_KEY, url.getParameter(TIMEOUT_KEY));
            }
            if (url.hasParameter(TOKEN_KEY)) {
                setAttachment(TOKEN_KEY, url.getParameter(TOKEN_KEY));
            }
            if (url.hasParameter(APPLICATION_KEY)) {
                setAttachment(APPLICATION_KEY, url.getParameter(APPLICATION_KEY));
            }
        }
        this.targetServiceUniqueName = invocation.getTargetServiceUniqueName();
    }

    public RpcInvocation(Invocation invocation) {
        this(invocation.getMethodName(), invocation.getServiceName(), invocation.getParameterTypes(),
                invocation.getArguments(), invocation.getAttachments(), invocation.getInvoker());
        this.targetServiceUniqueName = invocation.getTargetServiceUniqueName();
    }

    public RpcInvocation(Method method, String serviceName, Object[] arguments) {
        this(method, serviceName, arguments, null, null);
    }

    public RpcInvocation(Method method, String serviceName, Object[] arguments, Map<String, Object> attachment, Map<Object, Object> attributes) {
        this(method.getName(), serviceName, method.getParameterTypes(), arguments, attachment, null);
        this.returnType = method.getReturnType();
        this.attributes = attributes == null ? new HashMap<>() : attributes;
    }

    public RpcInvocation(String methodName, String serviceName, Class<?>[] parameterTypes, Object[] arguments) {
        this(methodName, serviceName, parameterTypes, arguments, null, null);
    }

    public RpcInvocation(String methodName, String serviceName, Class<?>[] parameterTypes, Object[] arguments, Map<String, Object> attachments) {
        this(methodName, serviceName, parameterTypes, arguments, attachments, null);
    }

    public RpcInvocation(String methodName, String serviceName, Class<?>[] parameterTypes, Object[] arguments, Map<String, Object> attachments, Invoker<?> invoker) {
        this.methodName = methodName;
        this.serviceName = serviceName;
        this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
        this.arguments = arguments == null ? new Object[0] : arguments;
        this.attachments = attachments == null ? new HashMap<String, Object>() : attachments;
        this.invoker = invoker;
        initParameterDesc();
    }

    private void initParameterDesc() {
        ServiceRepository repository = ApplicationModel.getServiceRepository();
        if (StringUtils.isNotEmpty(serviceName)) {
            ServiceDescriptor serviceDescriptor = repository.lookupService(serviceName);
            if (serviceDescriptor != null) {
                MethodDescriptor methodDescriptor = serviceDescriptor.getMethod(methodName, parameterTypes);
                if (methodDescriptor != null) {
                    this.parameterTypesDesc = methodDescriptor.getParamDesc();
                    this.compatibleParamSignatures = methodDescriptor.getCompatibleParamSignatures();
                    this.returnTypes = methodDescriptor.getReturnTypes();
                }
            }
        }

        if (parameterTypesDesc == null) {
            this.parameterTypesDesc = ReflectUtils.getDesc(this.getParameterTypes());
            this.compatibleParamSignatures = Stream.of(this.parameterTypes).map(Class::getName).toArray(String[]::new);
            this.returnTypes = RpcUtils.getReturnTypes(this);
        }
    }

    @Override
    public Invoker<?> getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker<?> invoker) {
        this.invoker = invoker;
    }

    public Object put(Object key, Object value) {
        return attributes.put(key, value);
    }

    public Object get(Object key) {
        return attributes.get(key);
    }

    @Override
    public Map<Object, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getTargetServiceUniqueName() {
        return targetServiceUniqueName;
    }

    public void setTargetServiceUniqueName(String targetServiceUniqueName) {
        this.targetServiceUniqueName = targetServiceUniqueName;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
    }

    public String getParameterTypesDesc() {
        return parameterTypesDesc;
    }

    public void setParameterTypesDesc(String parameterTypesDesc) {
        this.parameterTypesDesc = parameterTypesDesc;
    }

    public String[] getCompatibleParamSignatures() {
        return compatibleParamSignatures;
    }

    // parameter signatures can be set independently, it is useful when the service type is not found on caller side and
    // the invocation is not generic invocation either.
    public void setCompatibleParamSignatures(String[] compatibleParamSignatures) {
        this.compatibleParamSignatures = compatibleParamSignatures;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    public void setArguments(Object[] arguments) {
        this.arguments = arguments == null ? new Object[0] : arguments;
    }

    @Override
    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, Object> attachments) {
        this.attachments = attachments == null ? new HashMap<String, Object>() : attachments;
    }

    @Override
    public void setAttachment(String key, Object value) {
        if (attachments == null) {
            attachments = new HashMap<String, Object>();
        }
        attachments.put(key, value);
    }

    @Override
    public void setAttachmentIfAbsent(String key, Object value) {
        if (attachments == null) {
            attachments = new HashMap<String, Object>();
        }
        if (!attachments.containsKey(key)) {
            attachments.put(key, value);
        }
    }

    public void addAttachments(Map<String, Object> attachments) {
        if (attachments == null) {
            return;
        }
        if (this.attachments == null) {
            this.attachments = new HashMap<String, Object>();
        }
        this.attachments.putAll(attachments);
    }

    public void addAttachmentsIfAbsent(Map<String, Object> attachments) {
        if (attachments == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            setAttachmentIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Object getAttachment(String key) {
        if (attachments == null) {
            return null;
        }
        return attachments.get(key);
    }

    @Override
    public Object getAttachment(String key, Object defaultValue) {
        if (attachments == null) {
            return defaultValue;
        }
        Object value = attachments.get(key);
        if (null == value) {
            return defaultValue;
        }
        return value;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public Type[] getReturnTypes() {
        return returnTypes;
    }

    public void setReturnTypes(Type[] returnTypes) {
        this.returnTypes = returnTypes;
    }

    public InvokeMode getInvokeMode() {
        return invokeMode;
    }

    public void setInvokeMode(InvokeMode invokeMode) {
        this.invokeMode = invokeMode;
    }

    @Override
    public String toString() {
        return "RpcInvocation [methodName=" + methodName + ", parameterTypes="
                + Arrays.toString(parameterTypes) + ", arguments=" + Arrays.toString(arguments)
                + ", attachments=" + attachments + "]";
    }

}
