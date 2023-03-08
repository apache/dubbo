package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.URL;
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
import org.apache.dubbo.security.cert.rule.AuthorizationMatcher;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

@Activate(group = "provider")
public class SecurityFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final FrameworkModel frameworkModel;

    public SecurityFilter(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Channel channel = null;
        Object channelObj = invocation.getAttributes().get("channel");
        if (channelObj instanceof Channel) {
            channel = (Channel) channelObj;
        }

        if (channel == null) {
            return invoker.invoke(invocation);
        }

        URL peerPrincipals = null;
        URL localPrincipals = null;

        Object peerPrincipalsObj = channel.getAttribute("peerPrincipals");
        Object localPrincipalsObj = channel.getAttribute("localPrincipals");

        if (peerPrincipalsObj instanceof URL) {
            peerPrincipals = (URL) peerPrincipalsObj;
        }
        if (localPrincipalsObj instanceof URL) {
            localPrincipals = (URL) localPrincipalsObj;
        }

        if (peerPrincipals == null || localPrincipals == null) {
            Object sslSessionObj = invocation.getAttributes().get("dubbo.ssl.session");
            if (sslSessionObj instanceof SSLSession) {
                try {
                    Certificate[] peerCertificates = ((SSLSession) sslSessionObj).getPeerCertificates();
                    for (Certificate peerCertificate : peerCertificates) {
                        if (peerCertificate instanceof X509Certificate) {
                            X509Certificate x509Certificate = (X509Certificate) peerCertificate;
                            Collection<List<?>> sans = x509Certificate.getSubjectAlternativeNames();
                            for (List<?> san : sans) {
                                for (Object s : san) {
                                    if (s instanceof String && ((String) s).startsWith("dubbo://")) {
                                        peerPrincipals = URL.valueOf((String) s);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    Certificate[] localCertificates = ((SSLSession) sslSessionObj).getLocalCertificates();
                    for (Certificate localCertificate : localCertificates) {
                        if (localCertificate instanceof X509Certificate) {
                            X509Certificate x509Certificate = (X509Certificate) localCertificate;
                            Collection<List<?>> sans = x509Certificate.getSubjectAlternativeNames();
                            for (List<?> san : sans) {
                                for (Object s : san) {
                                    if (s instanceof String && ((String) s).startsWith("dubbo://")) {
                                        localPrincipals = URL.valueOf((String) s);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (SSLPeerUnverifiedException | CertificateParsingException e) {
                    logger.info("Failed to get peer certificate", e);
                }
            }
        }

        if (peerPrincipals == null || localPrincipals == null) {
            return invoker.invoke(invocation);
        }

        RuleManager ruleManager = frameworkModel.getBeanFactory().getBean(RuleManager.class);
        if (ruleManager == null) {
            return invoker.invoke(invocation);
        }

        List rules = ruleManager.getRules("authorization/v1beta1");
        if (rules == null || rules.isEmpty()) {
            return invoker.invoke(invocation);
        }

        if (AuthorizationMatcher.match(rules, peerPrincipals, localPrincipals, invocation)) {
            return invoker.invoke(invocation);
        } else {
            return AsyncRpcResult.newDefaultAsyncResult(new RpcException("Unauthorized"), invocation);
        }
    }
}
