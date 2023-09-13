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

package org.apache.dubbo.remoting.http12.aot;

import org.apache.dubbo.aot.api.MemberCategory;
import org.apache.dubbo.aot.api.ReflectionTypeDescriberRegistrar;
import org.apache.dubbo.aot.api.TypeDescriber;
import org.apache.dubbo.remoting.http12.netty4.HttpWriteQueueHandler;
import org.apache.dubbo.remoting.http12.netty4.h1.NettyHttp1Codec;
import org.apache.dubbo.remoting.http12.netty4.h2.NettyHttp2FrameCodec;
import org.apache.dubbo.remoting.http12.netty4.h2.NettyHttp2FrameHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Http12ReflectionTypeDescriberRegistrar implements ReflectionTypeDescriberRegistrar {

    @Override
    public List<TypeDescriber> getTypeDescribers() {
        List<TypeDescriber> typeDescribers = new ArrayList<>();
        typeDescribers.add(buildTypeDescriberWithPublicMethod(HttpWriteQueueHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(NettyHttp1Codec.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(NettyHttp2FrameCodec.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(NettyHttp2FrameHandler.class));
        return typeDescribers;
    }

    private TypeDescriber buildTypeDescriberWithPublicMethod(Class<?> cl) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_PUBLIC_METHODS);
        return new TypeDescriber(cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }
}
