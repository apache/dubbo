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
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;

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

        Endpoint endpoint = null;
        invocation.getAttributes().put("endpoint", endpoint);

        return invoker.invoke(invocation);
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
