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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.bytecode.NoSuchMethodException;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.support.Replier;

import java.lang.reflect.InvocationTargetException;

/**
 * RpcMessageHandler.
 */

public class RpcMessageHandler implements Replier<RpcMessage> {
    private final static ServiceProvider DEFAULT_PROVIDER = new ServiceProvider() {
        public Object getImplementation(String service) {
            String impl = service + "Impl";
            try {
                Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass(impl);
                return cl.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    };
    private ServiceProvider mProvider;

    public RpcMessageHandler() {
        this(DEFAULT_PROVIDER);
    }

    public RpcMessageHandler(ServiceProvider prov) {
        mProvider = prov;
    }

    public Class<RpcMessage> interest() {
        return RpcMessage.class;
    }

    public Object reply(ExchangeChannel channel, RpcMessage msg) throws RemotingException {
        String desc = msg.getMethodDesc();
        Object[] args = msg.getArguments();
        Object impl = mProvider.getImplementation(msg.getClassName());
        Wrapper wrap = Wrapper.getWrapper(impl.getClass());
        try {
            return new MockResult(wrap.invokeMethod(impl, desc, msg.getParameterTypes(), args));
        } catch (NoSuchMethodException e) {
            throw new RemotingException(channel, "Service method not found.");
        } catch (InvocationTargetException e) {
            return new MockResult(e.getTargetException());
        }

    }

    public interface ServiceProvider {
        Object getImplementation(String service);
    }

}
