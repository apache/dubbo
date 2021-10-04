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
package org.apache.dubbo.registry.dns.util;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.resolver.ResolvedAddressTypes;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.concurrent.Future;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;

import static io.netty.resolver.dns.DnsServerAddresses.sequential;

public class DNSResolver {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DnsNameResolver resolver;

    private static final EventLoopGroup GROUP = new NioEventLoopGroup(1);

    public DNSResolver(String nameserver, int port, int maxQueriesPerResolve) {
        this.resolver = newResolver(nameserver, port, maxQueriesPerResolve);
    }

    public ResolveResult resolve(String path) {
        ResolveResult recordList = new ResolveResult();

        try {
            Future<List<InetAddress>> hostFuture = resolver.resolveAll(path);
            Future<AddressedEnvelope<DnsResponse, InetSocketAddress>> srvFuture =
                    resolver.query(new DefaultDnsQuestion(path, DnsRecordType.SRV));

            try {
                recordList.getHostnameList()
                        .addAll(hostFuture
                                .sync().getNow()
                                .stream()
                                .map(InetAddress::getHostAddress)
                                .collect(Collectors.toList()));

                DnsResponse srvResponse = srvFuture.sync().getNow().content();
                for (int i = 0; i < srvResponse.count(DnsSection.ANSWER); i++) {
                    DnsRawRecord record = srvResponse.recordAt(DnsSection.ANSWER, i);
                    ByteBuf buf = record.content();
                    // Priority
                    buf.readUnsignedShort();
                    // Weight
                    buf.readUnsignedShort();
                    // Port
                    int port = buf.readUnsignedShort();
                    recordList.getPort().add(port);
                }

            } catch (InterruptedException e) {
                logger.warn("Waiting DNS resolve interrupted. " + e.getLocalizedMessage());
            }
        } catch (Throwable t) {
            if (t instanceof UnknownHostException) {
                if (logger.isInfoEnabled()) {
                    logger.info(t.getLocalizedMessage());
                }
            } else {
                logger.error(t.getLocalizedMessage());
            }
        }


        return recordList;
    }

    public void destroy() {
        resolver.close();
    }

    private static DnsNameResolver newResolver(String nameserver, int port, int maxQueriesPerResolve) {
        return new DnsNameResolverBuilder(GROUP.next())
                .channelType(NioDatagramChannel.class)
                .maxQueriesPerResolve(maxQueriesPerResolve)
                .decodeIdn(true)
                .optResourceEnabled(false)
                .ndots(1)
                .resolvedAddressTypes(ResolvedAddressTypes.IPV4_PREFERRED)
                // ignore cache
                .ttl(0, 1)
                .nameServerProvider((hostname) -> sequential(new InetSocketAddress(nameserver, port)).stream())
                .build();
    }
}
