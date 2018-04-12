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
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.PojoUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.service.GenericException;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * GenericImplInvokerFilter
 *
 * 服务消费者的泛化调用过滤器
 */
@Activate(group = Constants.CONSUMER, value = Constants.GENERIC_KEY, order = 20000)
public class GenericImplFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(GenericImplFilter.class);

    private static final Class<?>[] GENERIC_PARAMETER_TYPES = new Class<?>[]{String.class, String[].class, Object[].class};

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 获得 `generic` 配置项
        String generic = invoker.getUrl().getParameter(Constants.GENERIC_KEY);

        // 泛化实现的调用
        if (ProtocolUtils.isGeneric(generic)
                && !Constants.$INVOKE.equals(invocation.getMethodName())
                && invocation instanceof RpcInvocation) {
            RpcInvocation invocation2 = (RpcInvocation) invocation;
            String methodName = invocation2.getMethodName();
            Class<?>[] parameterTypes = invocation2.getParameterTypes();
            Object[] arguments = invocation2.getArguments();

            // 获得参数类型数组
            String[] types = new String[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                types[i] = ReflectUtils.getName(parameterTypes[i]);
            }

            Object[] args;
            // 【第一步】`bean` ，序列化参数，方法参数 => JavaBeanDescriptor
            if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                args = new Object[arguments.length];
                for (int i = 0; i < arguments.length; i++) {
                    args[i] = JavaBeanSerializeUtil.serialize(arguments[i], JavaBeanAccessor.METHOD);
                }
            // 【第一步】`true` ，序列化参数，仅有 Map => POJO
            } else {
                args = PojoUtils.generalize(arguments);
            }

            // 修改调用方法的名字为 `$invoke`
            invocation2.setMethodName(Constants.$INVOKE);
            // 设置调用方法的参数类型为 `GENERIC_PARAMETER_TYPES`
            invocation2.setParameterTypes(GENERIC_PARAMETER_TYPES);
            // 设置调用方法的参数数组，分别为方法名、参数类型数组、参数数组
            invocation2.setArguments(new Object[]{methodName, types, args});

            // 【第二步】RPC 调用
            Result result = invoker.invoke(invocation2);

            // 【第三步】反序列化正常结果
            if (!result.hasException()) {
                Object value = result.getValue();
                try {
                    // 【第三步】`bean` ，反序列化结果，JavaBeanDescriptor => 结果
                    if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                        if (value == null) {
                            return new RpcResult(null);
                        } else if (value instanceof JavaBeanDescriptor) {
                            return new RpcResult(JavaBeanSerializeUtil.deserialize((JavaBeanDescriptor) value));
                        } else { // 必须是 JavaBeanDescriptor 返回
                            throw new RpcException(
                                    new StringBuilder(64)
                                            .append("The type of result value is ")
                                            .append(value.getClass().getName())
                                            .append(" other than ")
                                            .append(JavaBeanDescriptor.class.getName())
                                            .append(", and the result is ")
                                            .append(value).toString());
                        }
                    } else {
                        // 获得对应的方法 Method 对象
                        Method method = invoker.getInterface().getMethod(methodName, parameterTypes);
                        //【第三步】`true` ，反序列化结果，仅有 Map => POJO
                        return new RpcResult(PojoUtils.realize(value, method.getReturnType(), method.getGenericReturnType()));
                    }
                } catch (NoSuchMethodException e) {
                    throw new RpcException(e.getMessage(), e);
                }
            // 【第三步】反序列化异常结果
            } else if (result.getException() instanceof GenericException) {
                GenericException exception = (GenericException) result.getException();
                try {
                    String className = exception.getExceptionClass();
                    Class<?> clazz = ReflectUtils.forName(className);
                    Throwable targetException = null;
                    Throwable lastException = null;
                    // 创建原始异常
                    try {
                        targetException = (Throwable) clazz.newInstance();
                    } catch (Throwable e) {
                        lastException = e;
                        for (Constructor<?> constructor : clazz.getConstructors()) {
                            try {
                                targetException = (Throwable) constructor.newInstance(new Object[constructor.getParameterTypes().length]);
                                break;
                            } catch (Throwable e1) {
                                lastException = e1;
                            }
                        }
                    }
                    // 设置异常的明细
                    if (targetException != null) {
                        try {
                            Field field = Throwable.class.getDeclaredField("detailMessage");
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            field.set(targetException, exception.getExceptionMessage());
                        } catch (Throwable e) {
                            logger.warn(e.getMessage(), e);
                        }
                        // 创建新的异常 RpcResult 对象
                        result = new RpcResult(targetException);
                    // 创建原始异常失败，抛出异常
                    } else if (lastException != null) {
                        throw lastException;
                    }
                } catch (Throwable e) { // 若发生异常，包装成 RpcException 异常，抛出。
                    throw new RpcException("Can not deserialize exception " + exception.getExceptionClass() + ", message: " + exception.getExceptionMessage(), e);
                }
            }
            // 返回 RpcResult 结果
            return result;
        }

        // 泛化引用的调用
        if (invocation.getMethodName().equals(Constants.$INVOKE) // 方法名为 `$invoke`
                && invocation.getArguments() != null
                && invocation.getArguments().length == 3
                && ProtocolUtils.isGeneric(generic)) {
            Object[] args = (Object[]) invocation.getArguments()[2];
            // `nativejava` ，校验方法参数都为 byte[]
            if (ProtocolUtils.isJavaGenericSerialization(generic)) {
                for (Object arg : args) {
                    if (!(byte[].class == arg.getClass())) {
                        error(byte[].class.getName(), arg.getClass().getName());
                    }
                }
            // `bean` ，校验方法参数为 JavaBeanDescriptor
            } else if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                for (Object arg : args) {
                    if (!(arg instanceof JavaBeanDescriptor)) {
                        error(JavaBeanDescriptor.class.getName(), arg.getClass().getName());
                    }
                }
            }

            // 通过隐式参数，传递 `generic` 配置项
            ((RpcInvocation) invocation).setAttachment(Constants.GENERIC_KEY, generic);
        }
        // 普通调用
        return invoker.invoke(invocation);
    }

    private void error(String expected, String actual) throws RpcException {
        throw new RpcException(
                new StringBuilder(32)
                        .append("Generic serialization [")
                        .append(Constants.GENERIC_SERIALIZATION_NATIVE_JAVA)
                        .append("] only support message type ")
                        .append(expected)
                        .append(" and your message type is ")
                        .append(actual).toString());
    }

}