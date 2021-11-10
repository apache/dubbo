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
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceModel;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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
    private String protocolServiceKey;

    private ServiceModel serviceModel;

    private String methodName;

    private String serviceName;

    private transient Class<?>[] parameterTypes;
    private String parameterTypesDesc;
    private String[] compatibleParamSignatures;

    private Object[] arguments;

    /**
     * Passed to the remote server during RPC call
     */
    private Map<String, Object> attachments;

    /**
     * Only used on the caller side, will not appear on the wire.
     */
    private transient Map<Object, Object> attributes = new HashMap<Object, Object>();

    private transient Invoker<?> invoker;

    private transient Class<?> returnType;

    private transient Type[] returnTypes;

    private transient InvokeMode invokeMode;

    public RpcInvocation() {
    }

    public RpcInvocation(Invocation invocation, Invoker<?> invoker) {
        this(invocation.getServiceModel(), invocation.getMethodName(), invocation.getServiceName(), invocation.getProtocolServiceKey(),
                invocation.getParameterTypes(), invocation.getArguments(), new HashMap<>(invocation.getObjectAttachments()),
                invocation.getInvoker(), invocation.getAttributes());
        if (invoker != null) {
            URL url = invoker.getUrl();
            setAttachment(PATH_KEY, url.getPath());
            if (url.hasParameter(INTERFACE_KEY)) {
                setAttachment(INTERFACE_KEY, url.getParameter(INTERFACE_KEY));
            }
            if (url.hasParameter(GROUP_KEY)) {
                setAttachment(GROUP_KEY, url.getGroup());
            }
            if (url.hasParameter(VERSION_KEY)) {
                setAttachment(VERSION_KEY, url.getVersion("0.0.0"));
            }
            if (url.hasParameter(TIMEOUT_KEY)) {
                setAttachment(TIMEOUT_KEY, url.getParameter(TIMEOUT_KEY));
            }
            if (url.hasParameter(TOKEN_KEY)) {
                setAttachment(TOKEN_KEY, url.getParameter(TOKEN_KEY));
            }
            if (url.hasParameter(APPLICATION_KEY)) {
                setAttachment(APPLICATION_KEY, url.getApplication());
            }
        }
        this.targetServiceUniqueName = invocation.getTargetServiceUniqueName();
        this.protocolServiceKey = invocation.getProtocolServiceKey();
    }

    public RpcInvocation(Invocation invocation) {
        this(invocation.getServiceModel(), invocation.getMethodName(), invocation.getServiceName(), invocation.getProtocolServiceKey(), invocation.getParameterTypes(),
                invocation.getArguments(), invocation.getObjectAttachments(), invocation.getInvoker(), invocation.getAttributes());
        this.targetServiceUniqueName = invocation.getTargetServiceUniqueName();
    }

    public RpcInvocation(ServiceModel serviceModel, Method method, String serviceName, String protocolServiceKey, Object[] arguments) {
        this(serviceModel, method, serviceName, protocolServiceKey, arguments, null, null);
    }

    @Deprecated
    public RpcInvocation(Method method, String serviceName, String protocolServiceKey, Object[] arguments) {
        this(null, method, serviceName, protocolServiceKey, arguments, null, null);
    }

    public RpcInvocation(ServiceModel serviceModel, Method method, String serviceName, String protocolServiceKey, Object[] arguments, Map<String, Object> attachment, Map<Object, Object> attributes) {
        this(null, serviceModel, method.getName(), serviceName, protocolServiceKey, method.getParameterTypes(), arguments, attachment, null, attributes);
    }

    @Deprecated
    public RpcInvocation(Method method, String serviceName, String protocolServiceKey, Object[] arguments, Map<String, Object> attachment, Map<Object, Object> attributes) {
        this(null, null, method.getName(), serviceName, protocolServiceKey, method.getParameterTypes(), arguments, attachment, null, attributes);
    }

    public RpcInvocation(ServiceModel serviceModel, String methodName, String serviceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments) {
        this(null, serviceModel, methodName, serviceName, protocolServiceKey, parameterTypes, arguments, null, null, null);
    }

    @Deprecated
    public RpcInvocation(String methodName, String serviceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments) {
        this(null, null, methodName, serviceName, protocolServiceKey, parameterTypes, arguments, null, null, null);
    }

    public RpcInvocation(ServiceModel serviceModel, String methodName, String serviceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments, Map<String, Object> attachments) {
        this(null, serviceModel, methodName, serviceName, protocolServiceKey, parameterTypes, arguments, attachments, null, null);
    }

    @Deprecated
    public RpcInvocation(String methodName, String serviceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments, Map<String, Object> attachments) {
        this(null, null, methodName, serviceName, protocolServiceKey, parameterTypes, arguments, attachments, null, null);
    }

    @Deprecated
    public RpcInvocation(String methodName, String serviceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments,
                         Map<String, Object> attachments, Invoker<?> invoker, Map<Object, Object> attributes) {
        this(null, null, methodName, serviceName, protocolServiceKey, parameterTypes, arguments, attachments, invoker, attributes);
    }

    public RpcInvocation(ServiceModel serviceModel, String methodName, String serviceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments,
                         Map<String, Object> attachments, Invoker<?> invoker, Map<Object, Object> attributes) {
        this(null, serviceModel, methodName, serviceName, protocolServiceKey, parameterTypes, arguments, attachments, invoker, attributes);
    }

    public RpcInvocation(String targetServiceUniqueName, ServiceModel serviceModel, String methodName, String serviceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments,
                         Map<String, Object> attachments, Invoker<?> invoker, Map<Object, Object> attributes) {
        this.targetServiceUniqueName = targetServiceUniqueName;
        this.serviceModel = serviceModel;
        this.methodName = methodName;
        this.serviceName = serviceName;
        this.protocolServiceKey = protocolServiceKey;
        this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
        this.arguments = arguments == null ? new Object[0] : arguments;
        this.attachments = attachments == null ? new HashMap<>() : attachments;
        this.attributes = attributes == null ? new HashMap<>() : attributes;
        this.invoker = invoker;
        initParameterDesc();
    }

    private void initParameterDesc() {
        AtomicReference<ServiceDescriptor> serviceDescriptor = new AtomicReference<>();
        if (serviceModel != null) {
            serviceDescriptor.set(serviceModel.getServiceModel());
        } else if (StringUtils.isNotEmpty(serviceName)){
            // TODO: Multi Instance compatible mode
            FrameworkModel.defaultModel()
                .getServiceRepository()
                .allProviderModels()
                .stream()
                .map(ProviderModel::getServiceModel)
                .filter(s->serviceName.equals(s.getServiceName()))
                .findFirst()
                .ifPresent(serviceDescriptor::set);
        }

        if (serviceDescriptor.get() != null) {
            MethodDescriptor methodDescriptor = serviceDescriptor.get().getMethod(methodName, parameterTypes);
            if (methodDescriptor != null) {
                this.parameterTypesDesc = methodDescriptor.getParamDesc();
                this.compatibleParamSignatures = methodDescriptor.getCompatibleParamSignatures();
                this.returnTypes = methodDescriptor.getReturnTypes();
                this.returnType = methodDescriptor.getReturnClass();
            }
        }


        if (parameterTypesDesc == null) {
            this.parameterTypesDesc = ReflectUtils.getDesc(this.getParameterTypes());
            this.compatibleParamSignatures = Stream.of(this.parameterTypes).map(Class::getName).toArray(String[]::new);
            this.returnTypes = RpcUtils.getReturnTypes(this);
            this.returnType = RpcUtils.getReturnType(this);
        }
    }

    @Override
    public Invoker<?> getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker<?> invoker) {
        this.invoker = invoker;
    }

    @Override
    public Object put(Object key, Object value) {
        return attributes.put(key, value);
    }

    @Override
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
    public String getProtocolServiceKey() {
        return protocolServiceKey;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    @Override
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
    public Map<String, Object> getObjectAttachments() {
        return attachments;
    }

    public void setObjectAttachments(Map<String, Object> attachments) {
        this.attachments = attachments == null ? new HashMap<>() : attachments;
    }

    @Override
    public void setAttachment(String key, String value) {
        setObjectAttachment(key, value);
    }

    @Deprecated
    @Override
    public Map<String, String> getAttachments() {
        return new AttachmentsAdapter.ObjectToStringMap(attachments);
    }

    @Deprecated
    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments == null ? new HashMap<>() : new HashMap<>(attachments);
    }

    @Override
    public void setAttachment(String key, Object value) {
        setObjectAttachment(key, value);
    }

    @Override
    public void setObjectAttachment(String key, Object value) {
        if (attachments == null) {
            attachments = new HashMap<>();
        }
        attachments.put(key, value);
    }

    @Override
    public void setAttachmentIfAbsent(String key, String value) {
        setObjectAttachmentIfAbsent(key, value);
    }

    @Override
    public void setAttachmentIfAbsent(String key, Object value) {
        setObjectAttachmentIfAbsent(key, value);
    }

    @Override
    public void setObjectAttachmentIfAbsent(String key, Object value) {
        if (attachments == null) {
            attachments = new HashMap<>();
        }
        if (!attachments.containsKey(key)) {
            attachments.put(key, value);
        }
    }

    @Deprecated
    public void addAttachments(Map<String, String> attachments) {
        if (attachments == null) {
            return;
        }
        if (this.attachments == null) {
            this.attachments = new HashMap<>();
        }
        this.attachments.putAll(attachments);
    }

    public void addObjectAttachments(Map<String, Object> attachments) {
        if (attachments == null) {
            return;
        }
        if (this.attachments == null) {
            this.attachments = new HashMap<>();
        }
        this.attachments.putAll(attachments);
    }

    @Deprecated
    public void addAttachmentsIfAbsent(Map<String, String> attachments) {
        if (attachments == null) {
            return;
        }
        for (Map.Entry<String, String> entry : attachments.entrySet()) {
            setAttachmentIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    public void addObjectAttachmentsIfAbsent(Map<String, Object> attachments) {
        if (attachments == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            setAttachmentIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    @Override
    @Deprecated
    public String getAttachment(String key) {
        if (attachments == null) {
            return null;
        }
        Object value = attachments.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    @Override
    public Object getObjectAttachment(String key) {
        if (attachments == null) {
            return null;
        }
        final Object val = attachments.get(key);
        if (val != null) {
            return val;
        }
        return attachments.get(key.toLowerCase(Locale.ROOT));
    }

    @Override
    @Deprecated
    public String getAttachment(String key, String defaultValue) {
        if (attachments == null) {
            return defaultValue;
        }
        Object value = attachments.get(key);
        if (value instanceof String) {
            String strValue = (String) value;
            if (StringUtils.isEmpty(strValue)) {
                return defaultValue;
            } else {
                return strValue;
            }
        }
        return null;
    }

    @Deprecated
    @Override
    public Object getObjectAttachment(String key, Object defaultValue) {
        if (attachments == null) {
            return defaultValue;
        }
        Object value = attachments.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public Object getObjectAttachmentWithoutConvert(String key) {
        if (attachments == null) {
            return null;
        }
        return attachments.get(key);
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
    public void setServiceModel(ServiceModel serviceModel) {
        this.serviceModel = serviceModel;
    }

    @Override
    public ServiceModel getServiceModel() {
        return serviceModel;
    }

    @Override
    public String toString() {
        return "RpcInvocation [methodName=" + methodName + ", parameterTypes="
                + Arrays.toString(parameterTypes) + ", arguments=" + Arrays.toString(arguments)
                + ", attachments=" + attachments + "]";
    }

}
