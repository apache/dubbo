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
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.beanutil.JavaBeanAccessor;
import org.apache.dubbo.common.beanutil.JavaBeanDescriptor;
import org.apache.dubbo.common.beanutil.JavaBeanSerializeUtil;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.PojoUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ListenableFilter;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.service.GenericException;
import org.apache.dubbo.rpc.support.ProtocolUtils;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static org.apache.dubbo.rpc.Constants.$INVOKE;
import static org.apache.dubbo.rpc.Constants.$INVOKE_ASYNC;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;

/**
 * GenericImplInvokerFilter
 */
@Activate(group = CommonConstants.CONSUMER, value = GENERIC_KEY, order = 20000)
public class GenericImplFilter extends ListenableFilter {

    private static final Logger logger = LoggerFactory.getLogger(GenericImplFilter.class);

    private static final Class<?>[] GENERIC_PARAMETER_TYPES = new Class<?>[]{String.class, String[].class, Object[].class};

    public GenericImplFilter() {
        super.listener = new GenericImplListener();
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String generic = invoker.getUrl().getParameter(GENERIC_KEY);
        if (ProtocolUtils.isGeneric(generic)
                && (!$INVOKE.equals(invocation.getMethodName()) && !$INVOKE_ASYNC.equals(invocation.getMethodName()))
                && invocation instanceof RpcInvocation) {
            RpcInvocation invocation2 = new RpcInvocation(invocation);
            String methodName = invocation2.getMethodName();
            Class<?>[] parameterTypes = invocation2.getParameterTypes();
            Object[] arguments = invocation2.getArguments();

            String[] types = new String[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                types[i] = ReflectUtils.getName(parameterTypes[i]);
            }

            Object[] args;
            if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                args = new Object[arguments.length];
                for (int i = 0; i < arguments.length; i++) {
                    args[i] = JavaBeanSerializeUtil.serialize(arguments[i], JavaBeanAccessor.METHOD);
                }
            } else {
                args = PojoUtils.generalize(arguments);
            }

            if (RpcUtils.isReturnTypeFuture(invocation)) {
                invocation2.setMethodName($INVOKE_ASYNC);
            } else {
                invocation2.setMethodName($INVOKE);
            }
            invocation2.setParameterTypes(GENERIC_PARAMETER_TYPES);
            invocation2.setArguments(new Object[]{methodName, types, args});
            return invoker.invoke(invocation2);
        } else if ((invocation.getMethodName().equals($INVOKE) || invocation.getMethodName().equals($INVOKE_ASYNC))
                && invocation.getArguments() != null
                && invocation.getArguments().length == 3
                && ProtocolUtils.isGeneric(generic)) {

            Object[] args = (Object[]) invocation.getArguments()[2];
            if (ProtocolUtils.isJavaGenericSerialization(generic)) {

                for (Object arg : args) {
                    if (!(byte[].class == arg.getClass())) {
                        error(generic, byte[].class.getName(), arg.getClass().getName());
                    }
                }
            } else if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                for (Object arg : args) {
                    if (!(arg instanceof JavaBeanDescriptor)) {
                        error(generic, JavaBeanDescriptor.class.getName(), arg.getClass().getName());
                    }
                }
            }

            invocation.setAttachment(
                    GENERIC_KEY, invoker.getUrl().getParameter(GENERIC_KEY));
        }
        return invoker.invoke(invocation);
    }

    private void error(String generic, String expected, String actual) throws RpcException {
        throw new RpcException("Generic serialization [" + generic + "] only support message type " + expected + " and your message type is " + actual);
    }

    static class GenericImplListener implements Listener {
        @Override
        public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
            String generic = invoker.getUrl().getParameter(GENERIC_KEY);
            String methodName = invocation.getMethodName();
            Class<?>[] parameterTypes = invocation.getParameterTypes();
            if (ProtocolUtils.isGeneric(generic)
                    && (!$INVOKE.equals(invocation.getMethodName()) && !$INVOKE_ASYNC.equals(invocation.getMethodName()))
                    && invocation instanceof RpcInvocation) {
                if (!appResponse.hasException()) {
                    Object value = appResponse.getValue();
                    try {
                        Method method = invoker.getInterface().getMethod(methodName, parameterTypes);
                        if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                            if (value == null) {
                                appResponse.setValue(value);
                            } else if (value instanceof JavaBeanDescriptor) {
                                appResponse.setValue(JavaBeanSerializeUtil.deserialize((JavaBeanDescriptor) value));
                            } else {
                                throw new RpcException("The type of result value is " + value.getClass().getName() + " other than " + JavaBeanDescriptor.class.getName() + ", and the result is " + value);
                            }
                        } else {
                            Type[] types = ReflectUtils.getReturnTypes(method);
                            appResponse.setValue(PojoUtils.realize(value, (Class<?>) types[0], types[1]));
                        }
                    } catch (NoSuchMethodException e) {
                        throw new RpcException(e.getMessage(), e);
                    }
                } else if (appResponse.getException() instanceof GenericException) {
                    GenericException exception = (GenericException) appResponse.getException();
                    try {
                        String className = exception.getExceptionClass();
                        Class<?> clazz = ReflectUtils.forName(className);
                        Throwable targetException = null;
                        Throwable lastException = null;
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
                            appResponse.setException(targetException);
                        } else if (lastException != null) {
                            throw lastException;
                        }
                    } catch (Throwable e) {
                        throw new RpcException("Can not deserialize exception " + exception.getExceptionClass() + ", message: " + exception.getExceptionMessage(), e);
                    }
                }
            }
        }

        @Override
        public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {

        }
    }

}
