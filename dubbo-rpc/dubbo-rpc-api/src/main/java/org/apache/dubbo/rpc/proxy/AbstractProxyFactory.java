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
package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ServiceModel;
import org.apache.dubbo.rpc.service.Destroyable;
import org.apache.dubbo.rpc.service.EchoService;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROXY_UNSUPPORTED_INVOKER;
import static org.apache.dubbo.rpc.Constants.INTERFACES;

/**
 * AbstractProxyFactory
 */
public abstract class AbstractProxyFactory implements ProxyFactory {
    private static final Class<?>[] INTERNAL_INTERFACES = new Class<?>[]{
        EchoService.class, Destroyable.class
    };

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractProxyFactory.class);

    @Override
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        return getProxy(invoker, false);
    }

    @Override
    public <T> T getProxy(Invoker<T> invoker, boolean generic) throws RpcException {
        // when compiling with native image, ensure that the order of the interfaces remains unchanged
        LinkedHashSet<Class<?>> interfaces = new LinkedHashSet<>();
        ClassLoader classLoader = getClassLoader(invoker);

        String config = invoker.getUrl().getParameter(INTERFACES);
        if (StringUtils.isNotEmpty(config)) {
            String[] types = COMMA_SPLIT_PATTERN.split(config);
            for (String type : types) {
                try {
                    interfaces.add(ReflectUtils.forName(classLoader, type));
                } catch (Throwable e) {
                    // ignore
                }

            }
        }

        Class<?> realInterfaceClass = null;
        if (generic) {
            try {
                // find the real interface from url
                String realInterface = invoker.getUrl().getParameter(Constants.INTERFACE);
                realInterfaceClass = ReflectUtils.forName(classLoader, realInterface);
                interfaces.add(realInterfaceClass);
            } catch (Throwable e) {
                // ignore
            }

            if (GenericService.class.equals(invoker.getInterface()) || !GenericService.class.isAssignableFrom(invoker.getInterface())) {
                interfaces.add(com.alibaba.dubbo.rpc.service.GenericService.class);
            }
        }

        interfaces.add(invoker.getInterface());
        interfaces.addAll(Arrays.asList(INTERNAL_INTERFACES));

        try {
            return getProxy(invoker, interfaces.toArray(new Class<?>[0]));
        } catch (Throwable t) {
            if (generic) {
                if (realInterfaceClass != null) {
                    interfaces.remove(realInterfaceClass);
                }
                interfaces.remove(invoker.getInterface());

                logger.error(PROXY_UNSUPPORTED_INVOKER, "", "", "Error occur when creating proxy. Invoker is in generic mode. Trying to create proxy without real interface class.", t);
                return getProxy(invoker, interfaces.toArray(new Class<?>[0]));
            } else {
                throw t;
            }
        }
    }

    private <T> ClassLoader getClassLoader(Invoker<T> invoker) {
        ServiceModel serviceModel = invoker.getUrl().getServiceModel();
        ClassLoader classLoader = null;
        if (serviceModel != null && serviceModel.getInterfaceClassLoader() != null) {
            classLoader = serviceModel.getInterfaceClassLoader();
        }
        if (classLoader == null) {
            classLoader = ClassUtils.getClassLoader();
        }
        return classLoader;
    }

    public static Class<?>[] getInternalInterfaces() {
        return INTERNAL_INTERFACES.clone();
    }

    public abstract <T> T getProxy(Invoker<T> invoker, Class<?>[] types);

}
