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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.proxy.jdk.JdkProxyFactory;

import java.util.Arrays;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROXY_FAILED;

public abstract class AbstractFallbackJdkProxyFactory extends AbstractProxyFactory {

    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(this.getClass());

    private final JdkProxyFactory jdkProxyFactory = new JdkProxyFactory();

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        try {
            return doGetInvoker(proxy, type, url);
        } catch (Throwable throwable) {
            // try fall back to JDK proxy factory
            String factoryName = getClass().getSimpleName();
            try {
                Invoker<T> invoker = jdkProxyFactory.getInvoker(proxy, type, url);
                logger.error(PROXY_FAILED, "", "", "Failed to generate invoker by " + factoryName + " failed. Fallback to use JDK proxy success. " +
                    "Interfaces: " + type, throwable);
                // log out error
                return invoker;
            } catch (Throwable fromJdk) {
                logger.error(PROXY_FAILED, "", "", "Failed to generate invoker by " + factoryName + " failed. Fallback to use JDK proxy is also failed. " +
                    "Interfaces: " + type + " Javassist Error.", throwable);
                logger.error(PROXY_FAILED, "", "", "Failed to generate invoker by " + factoryName + " failed. Fallback to use JDK proxy is also failed. " +
                    "Interfaces: " + type + " JDK Error.", fromJdk);
                throw throwable;
            }
        }
    }

    @Override
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        try {
            return doGetProxy(invoker, interfaces);
        } catch (Throwable throwable) {
            // try fall back to JDK proxy factory
            String factoryName = getClass().getSimpleName();
            try {
                T proxy = jdkProxyFactory.getProxy(invoker, interfaces);
                logger.error(PROXY_FAILED, "", "", "Failed to generate proxy by " + factoryName + " failed. Fallback to use JDK proxy success. " +
                    "Interfaces: " + Arrays.toString(interfaces), throwable);
                return proxy;
            } catch (Throwable fromJdk) {
                logger.error(PROXY_FAILED, "", "", "Failed to generate proxy by " + factoryName + " failed. Fallback to use JDK proxy is also failed. " +
                    "Interfaces: " + Arrays.toString(interfaces) + " Javassist Error.", throwable);
                logger.error(PROXY_FAILED, "", "", "Failed to generate proxy by " + factoryName + " failed. Fallback to use JDK proxy is also failed. " +
                    "Interfaces: " + Arrays.toString(interfaces) + " JDK Error.", fromJdk);
                throw throwable;
            }
        }
    }

    protected abstract <T> Invoker<T> doGetInvoker(T proxy, Class<T> type, URL url);

    protected abstract <T> T doGetProxy(Invoker<T> invoker, Class<?>[] types);
}
