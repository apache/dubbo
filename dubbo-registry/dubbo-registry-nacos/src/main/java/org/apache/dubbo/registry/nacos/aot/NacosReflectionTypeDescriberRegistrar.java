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
package org.apache.dubbo.registry.nacos.aot;

import org.apache.dubbo.aot.api.MemberCategory;
import org.apache.dubbo.aot.api.ReflectionTypeDescriberRegistrar;
import org.apache.dubbo.aot.api.TypeDescriber;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class NacosReflectionTypeDescriberRegistrar implements ReflectionTypeDescriberRegistrar {

    @Override
    public List<TypeDescriber> getTypeDescribers() {
        List<TypeDescriber> typeDescribers = new ArrayList<>();
        Class[] classesWithDeclared = {com.alibaba.nacos.api.ability.ClientAbilities.class, com.alibaba.nacos.api.config.ability.ClientConfigAbility.class, com.alibaba.nacos.api.config.remote.request.AbstractConfigRequest.class, com.alibaba.nacos.api.config.remote.request.ConfigBatchListenRequest.class, com.alibaba.nacos.api.config.remote.request.ConfigBatchListenRequest.ConfigListenContext.class, com.alibaba.nacos.api.config.remote.request.ConfigPublishRequest.class, com.alibaba.nacos.api.config.remote.request.ConfigQueryRequest.class, com.alibaba.nacos.api.config.remote.response.ConfigChangeBatchListenResponse.class, com.alibaba.nacos.api.config.remote.response.ConfigChangeBatchListenResponse.ConfigContext.class, com.alibaba.nacos.api.config.remote.response.ConfigPublishResponse.class, com.alibaba.nacos.api.config.remote.response.ConfigQueryResponse.class, com.alibaba.nacos.api.naming.ability.ClientNamingAbility.class, com.alibaba.nacos.api.naming.pojo.Instance.class, com.alibaba.nacos.api.naming.pojo.ServiceInfo.class, com.alibaba.nacos.api.naming.remote.request.AbstractNamingRequest.class, com.alibaba.nacos.api.naming.remote.request.InstanceRequest.class, com.alibaba.nacos.api.naming.remote.request.NotifySubscriberRequest.class, com.alibaba.nacos.api.naming.remote.request.ServiceQueryRequest.class, com.alibaba.nacos.api.naming.remote.request.SubscribeServiceRequest.class, com.alibaba.nacos.api.naming.remote.response.InstanceResponse.class, com.alibaba.nacos.api.naming.remote.response.NotifySubscriberResponse.class, com.alibaba.nacos.api.naming.remote.response.QueryServiceResponse.class, com.alibaba.nacos.api.naming.remote.response.SubscribeServiceResponse.class, com.alibaba.nacos.api.remote.ability.ClientRemoteAbility.class, com.alibaba.nacos.api.remote.request.ConnectionSetupRequest.class, com.alibaba.nacos.api.remote.request.HealthCheckRequest.class, com.alibaba.nacos.api.remote.request.InternalRequest.class, com.alibaba.nacos.api.remote.request.Request.class, com.alibaba.nacos.api.remote.request.ServerCheckRequest.class, com.alibaba.nacos.api.remote.request.ServerRequest.class, com.alibaba.nacos.api.remote.response.HealthCheckResponse.class, com.alibaba.nacos.api.remote.response.Response.class, com.alibaba.nacos.api.remote.response.ServerCheckResponse.class, com.alibaba.nacos.common.remote.TlsConfig.class, com.alibaba.nacos.common.remote.client.RpcClientTlsConfig.class};
        Class[] classesWithMethods = {com.alibaba.nacos.api.grpc.auto.Metadata.class, com.alibaba.nacos.api.grpc.auto.Metadata.Builder.class, com.alibaba.nacos.api.grpc.auto.Payload.class, com.alibaba.nacos.api.grpc.auto.Payload.Builder.class, com.alibaba.nacos.api.naming.NamingService.class, com.alibaba.nacos.api.remote.Payload.class, com.alibaba.nacos.client.auth.impl.NacosClientAuthServiceImpl.class, com.alibaba.nacos.client.auth.ram.RamClientAuthServiceImpl.class, com.alibaba.nacos.client.config.NacosConfigService.class, com.alibaba.nacos.client.naming.NacosNamingService.class, com.alibaba.nacos.common.notify.DefaultPublisher.class, com.alibaba.nacos.shaded.com.google.protobuf.Any.class, com.alibaba.nacos.shaded.com.google.protobuf.Any.Builder.class, com.alibaba.nacos.shaded.com.google.protobuf.ExtensionRegistry.class, com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.buffer.AbstractByteBufAllocator.class, com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.ChannelDuplexHandler.class, com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.ChannelInboundHandlerAdapter.class, com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel.class, com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.handler.codec.ByteToMessageDecoder.class, com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2ConnectionHandler.class, com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.ReferenceCountUtil.class};
        Class[] classesWithFields = {com.alibaba.nacos.api.PropertyKeyConst.class, com.alibaba.nacos.shaded.com.google.common.util.concurrent.AbstractFuture.class, com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.buffer.AbstractReferenceCountedByteBuf.class};
        Class[] classesWithDefault = {com.alibaba.nacos.shaded.io.grpc.internal.DnsNameResolverProvider.class, com.alibaba.nacos.shaded.io.grpc.internal.PickFirstLoadBalancerProvider.class};
        String[] privateClasses = {"com.alibaba.nacos.shaded.com.google.common.util.concurrent.AbstractFuture$Waiter", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.AbstractNettyHandler", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.NettyClientHandler", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.WriteBufferingAndExceptionHandler", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.DefaultChannelPipeline$HeadContext", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.DefaultChannelPipeline$TailContext", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueColdProducerFields", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueConsumerFields", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueProducerFields", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueConsumerIndexField", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerIndexField", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerLimitField"};
        for (Class className : classesWithDeclared) {
            typeDescribers.add(buildTypeDescriberWithDeclared(className));
        }
        for (Class className : classesWithMethods) {
            typeDescribers.add(buildTypeDescriberWithMethods(className));
        }
        for (Class className : classesWithFields) {
            typeDescribers.add(buildTypeDescriberWithFields(className));
        }
        for (Class className : classesWithDefault) {
            typeDescribers.add(buildTypeDescriberWithDefault(className));
        }
        for (String className : privateClasses) {
            typeDescribers.add(buildTypeDescriberWithDeclared(className));
        }
        return typeDescribers;
    }

    private TypeDescriber buildTypeDescriberWithDeclared(Class<?> cl) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_DECLARED_METHODS);
        memberCategories.add(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        memberCategories.add(MemberCategory.DECLARED_FIELDS);
        return new TypeDescriber(cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }

    private TypeDescriber buildTypeDescriberWithFields(Class<?> cl) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.DECLARED_FIELDS);
        return new TypeDescriber(cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }

    private TypeDescriber buildTypeDescriberWithMethods(Class<?> cl) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_DECLARED_METHODS);
        memberCategories.add(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        return new TypeDescriber(cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }

    private TypeDescriber buildTypeDescriberWithDefault(Class<?> cl) {
        return new TypeDescriber(cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    private TypeDescriber buildTypeDescriberWithDeclared(String className) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_DECLARED_METHODS);
        memberCategories.add(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        memberCategories.add(MemberCategory.DECLARED_FIELDS);
        return new TypeDescriber(className, null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }
    
    private TypeDescriber buildTypeDescriberWithFields(String className) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.DECLARED_FIELDS);
        return new TypeDescriber(className, null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }

    private TypeDescriber buildTypeDescriberWithMethods(String className) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_DECLARED_METHODS);
        memberCategories.add(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        return new TypeDescriber(className, null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }

    private TypeDescriber buildTypeDescriberWithDefault(String className) {
        return new TypeDescriber(className, null, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }


}
