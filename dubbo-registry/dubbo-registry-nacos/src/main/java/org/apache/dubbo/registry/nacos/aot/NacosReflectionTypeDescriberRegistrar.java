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
import com.alibaba.nacos.api.PropertyKeyConst;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class NacosReflectionTypeDescriberRegistrar implements ReflectionTypeDescriberRegistrar {

    @Override
    public List<TypeDescriber> getTypeDescribers() {
        List<TypeDescriber> typeDescribers = new ArrayList<>();
        String[] classesWithDeclared = {"com.alibaba.nacos.api.ability.ClientAbilities", "com.alibaba.nacos.api.config.ability.ClientConfigAbility", "com.alibaba.nacos.api.config.remote.request.AbstractConfigRequest", "com.alibaba.nacos.api.config.remote.request.ConfigBatchListenRequest", "com.alibaba.nacos.api.config.remote.request.ConfigBatchListenRequest$ConfigListenContext", "com.alibaba.nacos.api.config.remote.request.ConfigPublishRequest", "com.alibaba.nacos.api.config.remote.request.ConfigQueryRequest", "com.alibaba.nacos.api.config.remote.response.ConfigChangeBatchListenResponse", "com.alibaba.nacos.api.config.remote.response.ConfigChangeBatchListenResponse$ConfigContext", "com.alibaba.nacos.api.config.remote.response.ConfigPublishResponse", "com.alibaba.nacos.api.config.remote.response.ConfigQueryResponse", "com.alibaba.nacos.api.naming.ability.ClientNamingAbility", "com.alibaba.nacos.api.naming.pojo.Instance", "com.alibaba.nacos.api.naming.pojo.ServiceInfo", "com.alibaba.nacos.api.naming.remote.request.AbstractNamingRequest", "com.alibaba.nacos.api.naming.remote.request.InstanceRequest", "com.alibaba.nacos.api.naming.remote.request.NotifySubscriberRequest", "com.alibaba.nacos.api.naming.remote.request.ServiceQueryRequest", "com.alibaba.nacos.api.naming.remote.request.SubscribeServiceRequest", "com.alibaba.nacos.api.naming.remote.response.InstanceResponse", "com.alibaba.nacos.api.naming.remote.response.NotifySubscriberResponse", "com.alibaba.nacos.api.naming.remote.response.QueryServiceResponse", "com.alibaba.nacos.api.naming.remote.response.SubscribeServiceResponse", "com.alibaba.nacos.api.remote.ability.ClientRemoteAbility", "com.alibaba.nacos.api.remote.request.ConnectionSetupRequest", "com.alibaba.nacos.api.remote.request.HealthCheckRequest", "com.alibaba.nacos.api.remote.request.InternalRequest", "com.alibaba.nacos.api.remote.request.Request", "com.alibaba.nacos.api.remote.request.ServerCheckRequest", "com.alibaba.nacos.api.remote.request.ServerRequest", "com.alibaba.nacos.api.remote.response.HealthCheckResponse", "com.alibaba.nacos.api.remote.response.Response", "com.alibaba.nacos.api.remote.response.ServerCheckResponse", "com.alibaba.nacos.common.remote.TlsConfig", "com.alibaba.nacos.common.remote.client.RpcClientTlsConfig"};
        String[] classesWithMethods =  {"com.alibaba.nacos.api.grpc.auto.Metadata", "com.alibaba.nacos.api.grpc.auto.Metadata$Builder", "com.alibaba.nacos.api.grpc.auto.Payload", "com.alibaba.nacos.api.grpc.auto.Payload$Builder", "com.alibaba.nacos.api.naming.NamingService", "com.alibaba.nacos.api.remote.Payload", "com.alibaba.nacos.client.auth.impl.NacosClientAuthServiceImpl", "com.alibaba.nacos.client.auth.ram.RamClientAuthServiceImpl", "com.alibaba.nacos.client.config.NacosConfigService", "com.alibaba.nacos.client.naming.NacosNamingService", "com.alibaba.nacos.common.notify.DefaultPublisher", "com.alibaba.nacos.shaded.com.google.protobuf.Any", "com.alibaba.nacos.shaded.com.google.protobuf.Any$Builder", "com.alibaba.nacos.shaded.com.google.protobuf.ExtensionRegistry", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.AbstractNettyHandler", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.NettyClientHandler", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.ProtocolNegotiators$GrpcNegotiationHandler", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.ProtocolNegotiators$ProtocolNegotiationHandler", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.ProtocolNegotiators$WaitUntilActiveHandler", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.grpc.netty.WriteBufferingAndExceptionHandler", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.buffer.AbstractByteBufAllocator", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.ChannelDuplexHandler", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.ChannelInboundHandlerAdapter", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.DefaultChannelPipeline$HeadContext", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.DefaultChannelPipeline$TailContext", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.handler.codec.ByteToMessageDecoder", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2ConnectionHandler", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.ReferenceCountUtil"};
        String[] classesWithFields = {"com.alibaba.nacos.api.PropertyKeyConst", "com.alibaba.nacos.shaded.com.google.common.util.concurrent.AbstractFuture", "com.alibaba.nacos.shaded.com.google.common.util.concurrent.AbstractFuture$Waiter", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.buffer.AbstractReferenceCountedByteBuf", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueColdProducerFields", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueConsumerFields", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueProducerFields", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueConsumerIndexField", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerIndexField", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerLimitField"};
        String[] classesWithDefault = {"com.alibaba.nacos.shaded.io.grpc.census.InternalCensusStatsAccessor", "com.alibaba.nacos.shaded.io.grpc.census.InternalCensusTracingAccessor", "com.alibaba.nacos.shaded.io.grpc.internal.DnsNameResolverProvider", "com.alibaba.nacos.shaded.io.grpc.internal.JndiResourceResolverFactory", "com.alibaba.nacos.shaded.io.grpc.internal.PickFirstLoadBalancerProvider", "com.alibaba.nacos.shaded.io.grpc.netty.NettyChannelProvider", "com.alibaba.nacos.shaded.io.grpc.netty.UdsNettyChannelProvider", "com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.channel.epoll.Epoll", "com.alibaba.nacos.shaded.io.grpc.okhttp.OkHttpChannelProvider", "com.alibaba.nacos.shaded.io.grpc.override.ContextStorageOverride", "com.alibaba.nacos.shaded.io.grpc.util.SecretRoundRobinLoadBalancerProvider$Provider", "com.alibaba.nacos.shaded.io.perfmark.impl.SecretPerfMarkImpl$PerfMarkImpl"};
        for (String className : classesWithDeclared) {
            typeDescribers.add(buildTypeDescriberWithDeclared(className));
        }
        for (String className : classesWithMethods) {
            typeDescribers.add(buildTypeDescriberWithMethods(className));
        }
        for (String className : classesWithFields) {
            typeDescribers.add(buildTypeDescriberWithFields(className));
        }
        for (String className : classesWithDefault) {
            typeDescribers.add(buildTypeDescriberWithDefault(className));
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
