package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.security.cert.rule.authorization.AuthorizationMatcher;
import org.apache.dubbo.security.cert.rule.authorization.AuthorizationPolicy;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.util.List;

@Activate(group = "provider", order = Integer.MIN_VALUE + 10000)
public class AuthenticationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final FrameworkModel frameworkModel;

    private volatile AuthorityRuleSync authorityRuleSync;

    public AuthenticationFilter(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        obtainAuthorityRuleSync();
        if (authorityRuleSync == null) {
            return invoker.invoke(invocation);
        }

        Endpoints endpoints = getEndpoints(invocation);

        if (endpoints == null) {
            return invoker.invoke(invocation);
        }

        List<AuthorizationPolicy> rules = authorityRuleSync.getLatestAuthorizationPolicies();
        if (rules == null || rules.isEmpty()) {
            return invoker.invoke(invocation);
        }

        if (AuthorizationMatcher.match(rules, endpoints.getPeerEndpoint(), endpoints.getLocalEndpoint(), invocation)) {
            return invoker.invoke(invocation);
        } else {
            return AsyncRpcResult.newDefaultAsyncResult(new RpcException("Unauthorized"), invocation);
        }
    }

    private static Endpoints getEndpoints(Invocation invocation) {
        Endpoints endpoints = null;

        Channel channel = null;
        Object channelObj = invocation.getAttributes().get("channel");
        if (channelObj instanceof Channel) {
            channel = (Channel) channelObj;
        }

        Object endpointsObj = null;
        InetSocketAddress localAddress = null;
        InetSocketAddress remoteAddress = null;

        if (channel != null) {
            endpointsObj = channel.getAttribute("endpoints");
            localAddress = channel.getLocalAddress();
            remoteAddress = channel.getRemoteAddress();
        }

        if (endpointsObj instanceof Endpoints) {
            endpoints = (Endpoints) endpointsObj;
        }

        if (endpoints == null) {
            Object sslSessionObj = invocation.getAttributes().get("dubbo.ssl.session");
            if (sslSessionObj instanceof SSLSession) {
                try {
                    Certificate[] peerCertificates = ((SSLSession) sslSessionObj).getPeerCertificates();
                    Endpoint peerEndpoint = Endpoint.decodeFromCertificates(peerCertificates, remoteAddress);

                    Certificate[] localCertificates = ((SSLSession) sslSessionObj).getLocalCertificates();
                    Endpoint localEndpoint = Endpoint.decodeFromCertificates(localCertificates, localAddress);

                    endpoints = new Endpoints(peerEndpoint, localEndpoint);

                    if (channel != null) {
                        channel.setAttribute("endpoints", endpoints);
                    }
                } catch (SSLPeerUnverifiedException | CertificateParsingException e) {
                    logger.info("Failed to get peer certificate", e);
                }
            }
        }
        return endpoints;
    }

    private static class Endpoints {
        private final Endpoint localEndpoint;
        private final Endpoint peerEndpoint;

        public Endpoints(Endpoint localEndpoint, Endpoint peerEndpoint) {
            this.localEndpoint = localEndpoint;
            this.peerEndpoint = peerEndpoint;
        }

        public Endpoint getLocalEndpoint() {
            return localEndpoint;
        }

        public Endpoint getPeerEndpoint() {
            return peerEndpoint;
        }
    }

    private void obtainAuthorityRuleSync() {
        if (authorityRuleSync == null) {
            authorityRuleSync = frameworkModel.getBeanFactory().getBean(AuthorityRuleSync.class);
        }
    }
}
