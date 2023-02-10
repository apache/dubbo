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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceModel;
import org.apache.dubbo.rpc.support.RpcUtils;

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

    private String interfaceName;

    private transient Class<?>[] parameterTypes;
    private String parameterTypesDesc;
    private String[] compatibleParamSignatures;

    private Object[] arguments;

    /**
     * Passed to the remote server during RPC call
     */
    private Map<String, Object> attachments;

    private final transient Lock attachmentLock = new ReentrantLock();

    /**
     * Only used on the caller side, will not appear on the wire.
     */
    private transient Map<Object, Object> attributes = Collections.synchronizedMap(new HashMap<>());

    private transient Invoker<?> invoker;

    private transient Class<?> returnType;

    private transient Type[] returnTypes;

    private transient InvokeMode invokeMode;

    private transient List<Invoker<?>> invokedInvokers = new LinkedList<>();

    /**
     * @deprecated only for test
     */
    @Deprecated
    public RpcInvocation() {
    }

    /**
     * Deep clone of an invocation
     *
     * @param invocation original invocation
     */
    public RpcInvocation(Invocation invocation) {
        this(invocation, null);
    }

    /**
     * Deep clone of an invocation & put some service params into attachment from invoker (will not change the invoker in invocation)
     *
     * @param invocation original invocation
     * @param invoker    target invoker
     */
    public RpcInvocation(Invocation invocation, Invoker<?> invoker) {
        this(invocation.getTargetServiceUniqueName(), invocation.getServiceModel(), invocation.getMethodName(), invocation.getServiceName(),
            invocation.getProtocolServiceKey(), invocation.getParameterTypes(), invocation.getArguments(),
            invocation.copyObjectAttachments(), invocation.getInvoker(), invocation.getAttributes(),
            invocation instanceof RpcInvocation ? ((RpcInvocation) invocation).getInvokeMode() : null);
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
    }

    /**
     * To create a brand-new invocation
     */
    public RpcInvocation(ServiceModel serviceModel, String methodName, String interfaceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments) {
        this(null, serviceModel, methodName, interfaceName, protocolServiceKey, parameterTypes, arguments, null, null, null, null);
    }

    /**
     * @deprecated deprecated, will be removed in 3.1.x
     */
    @Deprecated
    public RpcInvocation(ServiceModel serviceModel, Method method, String interfaceName, String protocolServiceKey, Object[] arguments) {
        this(null, serviceModel, method.getName(), interfaceName, protocolServiceKey, method.getParameterTypes(), arguments, null, null, null, null);
    }

    /**
     * @deprecated deprecated, will be removed in 3.1.x
     */
    @Deprecated
    public RpcInvocation(Method method, String interfaceName, String protocolServiceKey, Object[] arguments) {
        this(null, null, method.getName(), interfaceName, protocolServiceKey, method.getParameterTypes(), arguments, null, null, null, null);
    }

    /**
     * @deprecated deprecated, will be removed in 3.1.x
     */
    @Deprecated
    public RpcInvocation(ServiceModel serviceModel, Method method, String interfaceName, String protocolServiceKey, Object[] arguments, Map<String, Object> attachment, Map<Object, Object> attributes) {
        this(null, serviceModel, method.getName(), interfaceName, protocolServiceKey, method.getParameterTypes(), arguments, attachment, null, attributes, null);
    }

    /**
     * @deprecated deprecated, will be removed in 3.1.x
     */
    @Deprecated
    public RpcInvocation(Method method, String interfaceName, String protocolServiceKey, Object[] arguments, Map<String, Object> attachment, Map<Object, Object> attributes) {
        this(null, null, method.getName(), interfaceName, protocolServiceKey, method.getParameterTypes(), arguments, attachment, null, attributes, null);
    }

    /**
     * @deprecated deprecated, will be removed in 3.1.x
     */
    @Deprecated
    public RpcInvocation(String methodName, String interfaceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments) {
        this(null, null, methodName, interfaceName, protocolServiceKey, parameterTypes, arguments, null, null, null, null);
    }

    /**
     * @deprecated deprecated, will be removed in 3.1.x
     */
    @Deprecated
    public RpcInvocation(ServiceModel serviceModel, String methodName, String interfaceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments, Map<String, Object> attachments) {
        this(null, serviceModel, methodName, interfaceName, protocolServiceKey, parameterTypes, arguments, attachments, null, null, null);
    }

    /**
     * @deprecated deprecated, will be removed in 3.1.x
     */
    @Deprecated
    public RpcInvocation(String methodName, String interfaceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments, Map<String, Object> attachments) {
        this(null, null, methodName, interfaceName, protocolServiceKey, parameterTypes, arguments, attachments, null, null, null);
    }

    /**
     * @deprecated deprecated, will be removed in 3.1.x
     */
    @Deprecated
    public RpcInvocation(String methodName, String interfaceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments,
                         Map<String, Object> attachments, Invoker<?> invoker, Map<Object, Object> attributes) {
        this(null, null, methodName, interfaceName, protocolServiceKey, parameterTypes, arguments, attachments, invoker, attributes, null);
    }

    /**
     * @deprecated deprecated, will be removed in 3.1.x
     */
    @Deprecated
    public RpcInvocation(ServiceModel serviceModel, String methodName, String interfaceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments,
                         Map<String, Object> attachments, Invoker<?> invoker, Map<Object, Object> attributes) {
        this(null, serviceModel, methodName, interfaceName, protocolServiceKey, parameterTypes, arguments, attachments, invoker, attributes, null);
    }

    /**
     * @deprecated deprecated, will be removed in 3.1.x
     */
    @Deprecated
    public RpcInvocation(ServiceModel serviceModel, String methodName, String interfaceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments,
                         Map<String, Object> attachments, Invoker<?> invoker, Map<Object, Object> attributes, InvokeMode invokeMode) {
        this(null, serviceModel, methodName, interfaceName, protocolServiceKey, parameterTypes, arguments, attachments, invoker, attributes, invokeMode);
    }

    /**
     * To create a brand-new invocation
     */
    public RpcInvocation(String targetServiceUniqueName, ServiceModel serviceModel, String methodName, String interfaceName, String protocolServiceKey, Class<?>[] parameterTypes, Object[] arguments,
                         Map<String, Object> attachments, Invoker<?> invoker, Map<Object, Object> attributes, InvokeMode invokeMode) {
        this.targetServiceUniqueName = targetServiceUniqueName;
        this.serviceModel = serviceModel;
        this.methodName = methodName;
        this.interfaceName = interfaceName;
        this.protocolServiceKey = protocolServiceKey;
        this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
        this.arguments = arguments == null ? new Object[0] : arguments;
        this.attachments = attachments == null ? new HashMap<>() : attachments;
        this.attributes = attributes == null ? Collections.synchronizedMap(new HashMap<>()) : attributes;
        this.invoker = invoker;
        initParameterDesc();
        this.invokeMode = invokeMode;
    }

    private void initParameterDesc() {
        AtomicReference<ServiceDescriptor> serviceDescriptor = new AtomicReference<>();
        if (serviceModel != null) {
            serviceDescriptor.set(serviceModel.getServiceModel());
        } else if (StringUtils.isNotEmpty(interfaceName)) {
            // TODO: Multi Instance compatible mode
            FrameworkModel.defaultModel()
                .getServiceRepository()
                .allProviderModels()
                .stream()
                .map(ProviderModel::getServiceModel)
                .filter(s -> interfaceName.equals(s.getInterfaceName()))
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
    public void addInvokedInvoker(Invoker<?> invoker) {
        this.invokedInvokers.add(invoker);
    }

    @Override
    public List<Invoker<?>> getInvokedInvokers() {
        return this.invokedInvokers;
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
        return interfaceName;
    }

    public void setServiceName(String interfaceName) {
        this.interfaceName = interfaceName;
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
        try {
            attachmentLock.lock();
            if (attachments == null) {
                attachments = new HashMap<>();
            }
            return attachments;
        } finally {
            attachmentLock.unlock();
        }
    }

    @Override
    public Map<String, Object> copyObjectAttachments() {
        try {
            attachmentLock.lock();
            if (attachments == null) {
                return new HashMap<>();
            }
            return new HashMap<>(attachments);
        } finally {
            attachmentLock.unlock();
        }
    }

    @Override
    public void foreachAttachment(Consumer<Map.Entry<String, Object>> consumer) {
        try {
            attachmentLock.lock();
            if (attachments != null) {
                attachments.entrySet().forEach(consumer);
            }
        } finally {
            attachmentLock.unlock();
        }
    }

    public void setObjectAttachments(Map<String, Object> attachments) {
        try {
            attachmentLock.lock();
            this.attachments = attachments == null ? new HashMap<>() : attachments;
        } finally {
            attachmentLock.unlock();
        }
    }

    @Override
    public void setAttachment(String key, String value) {
        setObjectAttachment(key, value);
    }

    @Deprecated
    @Override
    public Map<String, String> getAttachments() {
        try {
            attachmentLock.lock();
            if (attachments == null) {
                attachments = new HashMap<>();
            }
            return new AttachmentsAdapter.ObjectToStringMap(attachments);
        } finally {
            attachmentLock.unlock();
        }
    }

    @Deprecated
    public void setAttachments(Map<String, String> attachments) {
        try {
            attachmentLock.lock();
            this.attachments = attachments == null ? new HashMap<>() : new HashMap<>(attachments);
        } finally {
            attachmentLock.unlock();
        }
    }

    @Override
    public void setAttachment(String key, Object value) {
        setObjectAttachment(key, value);
    }

    @Override
    public void setObjectAttachment(String key, Object value) {
        try {
            attachmentLock.lock();
            if (attachments == null) {
                attachments = new HashMap<>();
            }
            attachments.put(key, value);
        } finally {
            attachmentLock.unlock();
        }
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
        try {
            attachmentLock.lock();
            if (attachments == null) {
                attachments = new HashMap<>();
            }
            if (!attachments.containsKey(key)) {
                attachments.put(key, value);
            }
        } finally {
            attachmentLock.unlock();
        }
    }

    @Deprecated
    public void addAttachments(Map<String, String> attachments) {
        try {
            attachmentLock.lock();
            if (attachments == null) {
                return;
            }
            if (this.attachments == null) {
                this.attachments = new HashMap<>();
            }
            this.attachments.putAll(attachments);
        } finally {
            attachmentLock.unlock();
        }
    }

    public void addObjectAttachments(Map<String, Object> attachments) {
        try {
            attachmentLock.lock();
            if (attachments == null) {
                return;
            }
            if (this.attachments == null) {
                this.attachments = new HashMap<>();
            }
            this.attachments.putAll(attachments);
        } finally {
            attachmentLock.unlock();
        }
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
        try {
            attachmentLock.lock();
            if (attachments == null) {
                return null;
            }
            Object value = attachments.get(key);
            if (value instanceof String) {
                return (String) value;
            }
            return null;
        } finally {
            attachmentLock.unlock();
        }
    }

    @Override
    public Object getObjectAttachment(String key) {
        try {
            attachmentLock.lock();
            if (attachments == null) {
                return null;
            }
            final Object val = attachments.get(key);
            if (val != null) {
                return val;
            }
            return attachments.get(key.toLowerCase(Locale.ROOT));
        } finally {
            attachmentLock.unlock();
        }
    }

    @Override
    @Deprecated
    public String getAttachment(String key, String defaultValue) {
        try {
            attachmentLock.lock();
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
            return defaultValue;
        } finally {
            attachmentLock.unlock();
        }
    }

    @Deprecated
    @Override
    public Object getObjectAttachment(String key, Object defaultValue) {
        try {
            attachmentLock.lock();
            if (attachments == null) {
                return defaultValue;
            }
            Object value = attachments.get(key);
            if (value == null) {
                return defaultValue;
            }
            return value;
        } finally {
            attachmentLock.unlock();
        }
    }

    @Override
    public Object getObjectAttachmentWithoutConvert(String key) {
        try {
            attachmentLock.lock();
            if (attachments == null) {
                return null;
            }
            return attachments.get(key);
        } finally {
            attachmentLock.unlock();
        }
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
            + Arrays.toString(parameterTypes) + "]";
    }

}
