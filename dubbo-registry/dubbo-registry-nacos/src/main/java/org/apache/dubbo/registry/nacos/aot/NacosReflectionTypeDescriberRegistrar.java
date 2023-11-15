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

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ability.ClientAbilities;
import com.alibaba.nacos.api.config.ability.ClientConfigAbility;
import com.alibaba.nacos.api.config.remote.request.AbstractConfigRequest;
import com.alibaba.nacos.api.config.remote.request.ConfigBatchListenRequest;
import com.alibaba.nacos.api.config.remote.request.ConfigBatchListenRequest.ConfigListenContext;
import com.alibaba.nacos.api.config.remote.request.ConfigPublishRequest;
import com.alibaba.nacos.api.config.remote.request.ConfigQueryRequest;
import com.alibaba.nacos.api.config.remote.response.ConfigChangeBatchListenResponse;
import com.alibaba.nacos.api.config.remote.response.ConfigChangeBatchListenResponse.ConfigContext;
import com.alibaba.nacos.api.config.remote.response.ConfigPublishResponse;
import com.alibaba.nacos.api.config.remote.response.ConfigQueryResponse;
import com.alibaba.nacos.api.grpc.auto.Metadata;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.ability.ClientNamingAbility;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.api.naming.remote.request.AbstractNamingRequest;
import com.alibaba.nacos.api.naming.remote.request.InstanceRequest;
import com.alibaba.nacos.api.naming.remote.request.NotifySubscriberRequest;
import com.alibaba.nacos.api.naming.remote.request.ServiceQueryRequest;
import com.alibaba.nacos.api.naming.remote.request.SubscribeServiceRequest;
import com.alibaba.nacos.api.naming.remote.response.InstanceResponse;
import com.alibaba.nacos.api.naming.remote.response.NotifySubscriberResponse;
import com.alibaba.nacos.api.naming.remote.response.QueryServiceResponse;
import com.alibaba.nacos.api.naming.remote.response.SubscribeServiceResponse;
import com.alibaba.nacos.api.remote.ability.ClientRemoteAbility;
import com.alibaba.nacos.api.remote.request.ConnectionSetupRequest;
import com.alibaba.nacos.api.remote.request.HealthCheckRequest;
import com.alibaba.nacos.api.remote.request.InternalRequest;
import com.alibaba.nacos.api.remote.request.Request;
import com.alibaba.nacos.api.remote.request.ServerCheckRequest;
import com.alibaba.nacos.api.remote.request.ServerRequest;
import com.alibaba.nacos.api.remote.response.HealthCheckResponse;
import com.alibaba.nacos.api.remote.response.Response;
import com.alibaba.nacos.api.remote.response.ServerCheckResponse;
import com.alibaba.nacos.client.auth.impl.NacosClientAuthServiceImpl;
import com.alibaba.nacos.client.auth.ram.RamClientAuthServiceImpl;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.client.naming.NacosNamingService;
import com.alibaba.nacos.common.notify.DefaultPublisher;
import com.alibaba.nacos.common.remote.TlsConfig;
import com.alibaba.nacos.common.remote.client.RpcClientTlsConfig;
import com.alibaba.nacos.shaded.com.google.common.util.concurrent.AbstractFuture;
import com.alibaba.nacos.shaded.com.google.protobuf.Any;
import com.alibaba.nacos.shaded.com.google.protobuf.ExtensionRegistry;
import com.alibaba.nacos.shaded.io.grpc.internal.DnsNameResolverProvider;
import com.alibaba.nacos.shaded.io.grpc.internal.PickFirstLoadBalancerProvider;
import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.buffer.AbstractByteBufAllocator;
import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.buffer.AbstractReferenceCountedByteBuf;
import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.ChannelDuplexHandler;
import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.ChannelInboundHandlerAdapter;
import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel;
import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.handler.codec.ByteToMessageDecoder;
import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2ConnectionHandler;
import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.ReferenceCountUtil;

public class NacosReflectionTypeDescriberRegistrar implements ReflectionTypeDescriberRegistrar {

    @Override
    public List<TypeDescriber> getTypeDescribers() {
        List<TypeDescriber> typeDescribers = new ArrayList<>();
        Class[] classesWithDeclared = {
            ClientAbilities.class,
            ClientConfigAbility.class,
            AbstractConfigRequest.class,
            ConfigBatchListenRequest.class,
            ConfigListenContext.class,
            ConfigPublishRequest.class,
            ConfigQueryRequest.class,
            ConfigChangeBatchListenResponse.class,
            ConfigContext.class,
            ConfigPublishResponse.class,
            ConfigQueryResponse.class,
            ClientNamingAbility.class,
            Instance.class,
            ServiceInfo.class,
            AbstractNamingRequest.class,
            InstanceRequest.class,
            NotifySubscriberRequest.class,
            ServiceQueryRequest.class,
            SubscribeServiceRequest.class,
            InstanceResponse.class,
            NotifySubscriberResponse.class,
            QueryServiceResponse.class,
            SubscribeServiceResponse.class,
            ClientRemoteAbility.class,
            ConnectionSetupRequest.class,
            HealthCheckRequest.class,
            InternalRequest.class,
            Request.class,
            ServerCheckRequest.class,
            ServerRequest.class,
            HealthCheckResponse.class,
            Response.class,
            ServerCheckResponse.class,
            TlsConfig.class,
            RpcClientTlsConfig.class
        };
        Class[] classesWithMethods = {
            Metadata.class,
            com.alibaba.nacos.api.grpc.auto.Metadata.Builder.class,
            com.alibaba.nacos.api.grpc.auto.Payload.class,
            com.alibaba.nacos.api.grpc.auto.Payload.Builder.class,
            NamingService.class,
            com.alibaba.nacos.api.remote.Payload.class,
            NacosClientAuthServiceImpl.class,
            RamClientAuthServiceImpl.class,
            NacosConfigService.class,
            NacosNamingService.class,
            DefaultPublisher.class,
            Any.class,
            com.alibaba.nacos.shaded.com.google.protobuf.Any.Builder.class,
            ExtensionRegistry.class,
            AbstractByteBufAllocator.class,
            ChannelDuplexHandler.class,
            ChannelInboundHandlerAdapter.class,
            NioSocketChannel.class,
            ByteToMessageDecoder.class,
            Http2ConnectionHandler.class,
            ReferenceCountUtil.class
        };
        Class[] classesWithFields = {PropertyKeyConst.class, AbstractFuture.class, AbstractReferenceCountedByteBuf.class
        };
        Class[] classesWithDefault = {DnsNameResolverProvider.class, PickFirstLoadBalancerProvider.class};
        String[] privateClasses = {
            "com.alibaba.nacos.shaded.com.google.common.util.concurrent.AbstractFuture.Waiter",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.AbstractNettyHandler",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.NettyClientHandler",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.WriteBufferingAndExceptionHandler",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.DefaultChannelPipeline.HeadContext",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.DefaultChannelPipeline.TailContext",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueColdProducerFields",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueConsumerFields",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueProducerFields",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueConsumerIndexField",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerIndexField",
            "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerLimitField"
        };
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
        return new TypeDescriber(
                cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }

    private TypeDescriber buildTypeDescriberWithFields(Class<?> cl) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.DECLARED_FIELDS);
        return new TypeDescriber(
                cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }

    private TypeDescriber buildTypeDescriberWithMethods(Class<?> cl) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_DECLARED_METHODS);
        memberCategories.add(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        return new TypeDescriber(
                cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }

    private TypeDescriber buildTypeDescriberWithDefault(Class<?> cl) {
        return new TypeDescriber(
                cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
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
