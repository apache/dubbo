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
package org.apache.dubbo.remoting.transport.netty4.aot;

import org.apache.dubbo.aot.api.MemberCategory;
import org.apache.dubbo.aot.api.ReflectionTypeDescriberRegistrar;
import org.apache.dubbo.aot.api.TypeDescriber;
import org.apache.dubbo.remoting.transport.netty4.NettyChannelHandler;
import org.apache.dubbo.remoting.transport.netty4.NettyClientHandler;
import org.apache.dubbo.remoting.transport.netty4.NettyConnectionHandler;
import org.apache.dubbo.remoting.transport.netty4.NettyPortUnificationServerHandler;
import org.apache.dubbo.remoting.transport.netty4.NettyServerHandler;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslClientTlsHandler;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslServerTlsHandler;

import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Netty4ReflectionTypeDescriberRegistrar implements ReflectionTypeDescriberRegistrar {

    @Override
    public List<TypeDescriber> getTypeDescribers() {
        List<TypeDescriber> typeDescribers = new ArrayList<>();
        typeDescribers.add(buildTypeDescriberWithPublicMethod(NettyServerHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(SslServerTlsHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(NettyClientHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(SslClientTlsHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(SelectorProvider.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(NettyPortUnificationServerHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(NettyChannelHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(NettyConnectionHandler.class));
        return typeDescribers;
    }

    private TypeDescriber buildTypeDescriberWithPublicMethod(Class<?> cl) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_PUBLIC_METHODS);
        return new TypeDescriber(
                cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }
}
