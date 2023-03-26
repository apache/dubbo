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

package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ModuleModel;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

@Activate(group = "provider", order = Integer.MIN_VALUE + 9000)
public class AuthenticationProviderFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationProviderFilter.class);
    private volatile AuthorityIdentityFactory authorityIdentityFactory;
    private volatile AuthenticationGovernor authenticationGovernor;

    private final ModuleModel moduleModel;

    public AuthenticationProviderFilter(ModuleModel moduleModel) {
        this.moduleModel = moduleModel;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        obtainAuthorityIdentityFactory();
        obtainAuthenticationGovernor();
        if (authorityIdentityFactory == null || authenticationGovernor == null) {
            return invoker.invoke(invocation);
        }

        IdentityInfo identityInfo = authorityIdentityFactory.generateIdentity();

        Endpoints endpoints = getEndpoints(invocation, identityInfo);
        invocation.getAttributes().put("endpoints", endpoints);

        return invoker.invoke(invocation);
    }


    private static Endpoints getEndpoints(Invocation invocation, IdentityInfo identityInfo) {
        Channel channel = null;
        Object channelObj = invocation.getAttributes().get("channel");
        if (channelObj instanceof Channel) {
            channel = (Channel) channelObj;
        }

        InetSocketAddress localAddress = null;
        InetSocketAddress remoteAddress = null;

        if (channel != null) {
            localAddress = channel.getLocalAddress();
            remoteAddress = channel.getRemoteAddress();
        }

        String peerAuthorization = invocation.getAttachment("authorization");

        String peerCN = null;
        Object sslSessionObj = invocation.getAttributes().get("dubbo.ssl.session");
        if (sslSessionObj instanceof SSLSession) {
            try {
                Certificate[] peerCertificates = ((SSLSession) sslSessionObj).getPeerCertificates();
                for (Certificate certificate : peerCertificates) {
                    if (certificate instanceof X509Certificate) {
                        X509Certificate x509Certificate = (X509Certificate) certificate;
                        Principal subjectDN = x509Certificate.getSubjectDN();
                        String name = subjectDN.getName();
                        if (name.contains("CN=")) {
                            peerCN = name.split("CN=")[1].split(",")[0].trim();
                            break;
                        }
                    }
                }
            } catch (SSLPeerUnverifiedException e) {
                logger.info("Failed to get peer certificate", e);
            }
        }

        Endpoint peerEndpoint = JwtUtils.decodeEndpointFromJwt(peerAuthorization, peerCN, identityInfo.getTrustedTokenPublicKeys(), remoteAddress);
        Endpoint localEndpoint = JwtUtils.decodeEndpointFromJwt(identityInfo.getToken(), null, identityInfo.getTrustedTokenPublicKeys(), localAddress);
        return new Endpoints(localEndpoint, peerEndpoint);
    }


    private void obtainAuthorityIdentityFactory() {
        if (authorityIdentityFactory == null) {
            authorityIdentityFactory = moduleModel.getApplicationModel().getFrameworkModel()
                .getBeanFactory().getBean(AuthorityIdentityFactory.class);
        }
    }

    private void obtainAuthenticationGovernor() {
        if (authenticationGovernor == null) {
            authenticationGovernor = moduleModel.getApplicationModel().getFrameworkModel()
                .getBeanFactory().getBean(AuthenticationGovernor.class);
        }
    }
}
