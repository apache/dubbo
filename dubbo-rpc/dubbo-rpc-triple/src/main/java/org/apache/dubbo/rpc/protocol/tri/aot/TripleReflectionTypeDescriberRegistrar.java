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
package org.apache.dubbo.rpc.protocol.tri.aot;

import org.apache.dubbo.aot.api.MemberCategory;
import org.apache.dubbo.aot.api.ReflectionTypeDescriberRegistrar;
import org.apache.dubbo.aot.api.TypeDescriber;
import org.apache.dubbo.remoting.http12.ErrorResponse;
import org.apache.dubbo.remoting.http12.message.codec.CodecUtils;
import org.apache.dubbo.rpc.protocol.tri.h12.CompositeExceptionHandler;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.CompositeArgumentConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.CompositeArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.GeneralTypeConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.ContentNegotiator;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.DefaultRequestMappingRegistry;
import org.apache.dubbo.rpc.protocol.tri.route.DefaultRequestRouter;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleGoAwayHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleHttp2ClientResponseHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleServerConnectionHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleTailHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TripleReflectionTypeDescriberRegistrar implements ReflectionTypeDescriberRegistrar {
    @Override
    public List<TypeDescriber> getTypeDescribers() {
        List<TypeDescriber> typeDescribers = new ArrayList<>();
        typeDescribers.add(buildTypeDescriberWithPublicMethod(TripleCommandOutBoundHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(TripleTailHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(TripleServerConnectionHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(TripleGoAwayHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(TripleHttp2ClientResponseHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(ErrorResponse.class));
        typeDescribers.add(buildTypeDescriberWithDeclaredConstructors(DefaultRequestMappingRegistry.class));
        typeDescribers.add(buildTypeDescriberWithDeclaredConstructors(DefaultRequestRouter.class));
        typeDescribers.add(buildTypeDescriberWithDeclaredConstructors(ContentNegotiator.class));
        typeDescribers.add(buildTypeDescriberWithDeclaredConstructors(CompositeArgumentResolver.class));
        typeDescribers.add(buildTypeDescriberWithDeclaredConstructors(CompositeArgumentConverter.class));
        typeDescribers.add(buildTypeDescriberWithDeclaredConstructors(CompositeExceptionHandler.class));
        typeDescribers.add(buildTypeDescriberWithDeclaredConstructors(GeneralTypeConverter.class));
        typeDescribers.add(buildTypeDescriberWithDeclaredConstructors(CodecUtils.class));
        return typeDescribers;
    }

    private TypeDescriber buildTypeDescriberWithPublicMethod(Class<?> c) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_PUBLIC_METHODS);
        return new TypeDescriber(
                c.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }

    private TypeDescriber buildTypeDescriberWithDeclaredConstructors(Class<?> cl) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        return new TypeDescriber(
                cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }
}
