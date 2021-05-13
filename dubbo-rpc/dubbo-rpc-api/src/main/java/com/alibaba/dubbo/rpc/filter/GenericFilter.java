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
package com.alibaba.dubbo.rpc.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.beanutil.JavaBeanAccessor;
import com.alibaba.dubbo.common.beanutil.JavaBeanDescriptor;
import com.alibaba.dubbo.common.beanutil.JavaBeanSerializeUtil;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.common.utils.PojoUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.service.GenericException;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * GenericInvokerFilter.
 */
@Activate(group = Constants.PROVIDER, order = -20000)
public class GenericFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(GenericFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {
        if (inv.getMethodName().equals(Constants.$INVOKE)
                && inv.getArguments() != null
                && inv.getArguments().length == 3
                && !invoker.getInterface().equals(GenericService.class)) {
            String name = ((String) inv.getArguments()[0]).trim();
            String[] types = (String[]) inv.getArguments()[1];
            Object[] args = (Object[]) inv.getArguments()[2];
            try {
                Method method = ReflectUtils.findMethodByMethodSignature(invoker.getInterface(), name, types);
                Class<?>[] params = method.getParameterTypes();
                if (args == null) {
                    args = new Object[params.length];
                }
                String generic = inv.getAttachment(Constants.GENERIC_KEY);

                if (StringUtils.isBlank(generic)) {
                    generic = RpcContext.getContext().getAttachment(Constants.GENERIC_KEY);
                }

                if (StringUtils.isEmpty(generic)
                        || ProtocolUtils.isDefaultGenericSerialization(generic)) {
                    args = PojoUtils.realize(args, params, method.getGenericParameterTypes());
                } else if (ProtocolUtils.isJavaGenericSerialization(generic)) {
                    if (!nativeJavaSerializerEnabled()) {
                        String notice = "Trigger the safety barrier! " +
                                "Native Java Serializer is not allowed by default." +
                                "This means currently maybe being attacking by others. " +
                                "If you are sure this is a mistake, " +
                                "please set `" + Constants.ENABLE_NATIVE_JAVA_GENERIC_SERIALIZE + "` enable in configuration! " +
                                "Before doing so, please make sure you have configure JEP290 to prevent serialization attack.";
                        logger.error(notice);
                        throw new RpcException(new IllegalStateException(notice));
                    }

                    for (int i = 0; i < args.length; i++) {
                        if (byte[].class == args[i].getClass()) {
                            try {
                                UnsafeByteArrayInputStream is = new UnsafeByteArrayInputStream((byte[]) args[i]);
                                args[i] = ExtensionLoader.getExtensionLoader(Serialization.class)
                                        .getExtension(Constants.GENERIC_SERIALIZATION_NATIVE_JAVA)
                                        .deserialize(null, is).readObject();
                            } catch (Exception e) {
                                throw new RpcException("Deserialize argument [" + (i + 1) + "] failed.", e);
                            }
                        } else {
                            throw new RpcException(
                                    "Generic serialization [" +
                                            Constants.GENERIC_SERIALIZATION_NATIVE_JAVA +
                                            "] only support message type " +
                                            byte[].class +
                                            " and your message type is " +
                                            args[i].getClass());
                        }
                    }
                } else if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof JavaBeanDescriptor) {
                            args[i] = JavaBeanSerializeUtil.deserialize((JavaBeanDescriptor) args[i]);
                        } else {
                            throw new RpcException(
                                    "Generic serialization [" +
                                            Constants.GENERIC_SERIALIZATION_BEAN +
                                            "] only support message type " +
                                            JavaBeanDescriptor.class.getName() +
                                            " and your message type is " +
                                            args[i].getClass().getName());
                        }
                    }
                }
                Result result = invoker.invoke(new RpcInvocation(method, args, inv.getAttachments()));
                if (result.hasException()
                        && !(result.getException() instanceof GenericException)) {
                    return new RpcResult(new GenericException(result.getException()));
                }
                RpcResult rpcResult;
                if (ProtocolUtils.isJavaGenericSerialization(generic)) {
                    try {
                        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(512);
                        ExtensionLoader.getExtensionLoader(Serialization.class)
                                .getExtension(Constants.GENERIC_SERIALIZATION_NATIVE_JAVA)
                                .serialize(null, os).writeObject(result.getValue());
                        rpcResult = new RpcResult(os.toByteArray());
                    } catch (IOException e) {
                        throw new RpcException("Serialize result failed.", e);
                    }
                } else if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                    rpcResult = new RpcResult(JavaBeanSerializeUtil.serialize(result.getValue(), JavaBeanAccessor.METHOD));
                } else {
                    rpcResult = new RpcResult(PojoUtils.generalize(result.getValue()));
                }
                rpcResult.setAttachments(result.getAttachments());
                return rpcResult;
            } catch (NoSuchMethodException e) {
                throw new RpcException(e.getMessage(), e);
            } catch (ClassNotFoundException e) {
                throw new RpcException(e.getMessage(), e);
            }
        }
        return invoker.invoke(inv);
    }

    private boolean nativeJavaSerializerEnabled() {
        return Boolean.parseBoolean(System.getProperty(Constants.ENABLE_NATIVE_JAVA_GENERIC_SERIALIZE))
                || Boolean.parseBoolean(System.getenv(Constants.ENABLE_NATIVE_JAVA_GENERIC_SERIALIZE))
                || Boolean.parseBoolean(System.getenv(StringUtils.toOSStyleKey(Constants.ENABLE_NATIVE_JAVA_GENERIC_SERIALIZE)));
    }
}
