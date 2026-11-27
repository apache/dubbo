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
package org.apache.dubbo.xds.security;

import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.CertificateProviderInfo;

import javax.net.ssl.SNIHostName;

import java.io.IOException;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.TransportSocket;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CertificateValidationContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.SubjectAltNameMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.TlsParameters;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.StringMatcher;

/** Explicit certificate-provider TLS contract; unsupported security fields are rejected before ACK. */
public final class TlsPolicy {
    private static final TlsPolicy PLAINTEXT =
            new TlsPolicy(false, false, null, null, CertificateValidationContext.getDefaultInstance(), null);
    private final boolean tls;
    private final boolean requireClientCertificate;
    private final String identityProvider;
    private final String rootProvider;
    private final CertificateValidationContext validation;
    private final String serverName;

    private TlsPolicy(
            boolean tls,
            boolean requireClientCertificate,
            String identityProvider,
            String rootProvider,
            CertificateValidationContext validation,
            String serverName) {
        this.tls = tls;
        this.requireClientCertificate = requireClientCertificate;
        this.identityProvider = identityProvider;
        this.rootProvider = rootProvider;
        this.validation = validation;
        this.serverName = serverName == null || serverName.isEmpty() ? null : serverName;
    }

    public static TlsPolicy client(Cluster cluster, BootstrapInfo bootstrap) {
        if (cluster.getTransportSocketMatchesCount() > 0) {
            throw new IllegalArgumentException("Transport-socket matching is unsupported");
        }
        if (!cluster.hasTransportSocket()) {
            return PLAINTEXT;
        }
        try {
            TransportSocket socket = cluster.getTransportSocket();
            requireSocket(socket);
            UpstreamTlsContext context = socket.getTypedConfig().unpack(UpstreamTlsContext.class);
            SecurityFields.only(context, "common_tls_context", "sni");
            if (!context.getSni().isEmpty()) {
                new SNIHostName(context.getSni());
            }
            if (context.getAllowRenegotiation() || context.hasMaxSessionKeys()) {
                throw new IllegalArgumentException("Unsupported upstream TLS options");
            }
            return common(context.getCommonTlsContext(), bootstrap, false, context.getSni());
        } catch (IOException error) {
            throw new IllegalArgumentException("Invalid upstream TLS configuration", error);
        }
    }

    public static TlsPolicy server(FilterChain chain, BootstrapInfo bootstrap) {
        if (!chain.hasTransportSocket()) {
            return PLAINTEXT;
        }
        try {
            requireSocket(chain.getTransportSocket());
            DownstreamTlsContext context =
                    chain.getTransportSocket().getTypedConfig().unpack(DownstreamTlsContext.class);
            SecurityFields.only(context, "common_tls_context", "require_client_certificate");
            if (context.hasRequireSni()
                    || context.hasSessionTicketKeys()
                    || context.hasSessionTicketKeysSdsSecretConfig()) {
                throw new IllegalArgumentException("Unsupported downstream TLS options");
            }
            return common(
                    context.getCommonTlsContext(),
                    bootstrap,
                    context.getRequireClientCertificate().getValue(),
                    null);
        } catch (IOException error) {
            throw new IllegalArgumentException("Invalid downstream TLS configuration", error);
        }
    }

    private static void requireSocket(TransportSocket socket) {
        SecurityFields.only(socket, "name", "typed_config");
        if (!"envoy.transport_sockets.tls".equals(socket.getName()) || !socket.hasTypedConfig()) {
            throw new IllegalArgumentException("Unsupported xDS transport socket");
        }
    }

    private static TlsPolicy common(
            CommonTlsContext context, BootstrapInfo bootstrap, boolean requireClient, String serverName) {
        SecurityFields.only(
                context,
                "tls_params",
                "tls_certificate_provider_instance",
                "tls_certificate_certificate_provider_instance",
                "combined_validation_context",
                "validation_context",
                "validation_context_certificate_provider_instance",
                "alpn_protocols");
        for (String protocol : context.getAlpnProtocolsList()) {
            if (!"h2".equals(protocol)) {
                throw new IllegalArgumentException("Only h2 ALPN is supported for native Triple TLS");
            }
        }
        if (context.hasTlsParams() && !context.getTlsParams().equals(TlsParameters.getDefaultInstance())) {
            throw new IllegalArgumentException("Explicit TLS parameters require a supported native TLS policy");
        }
        if (context.getTlsCertificatesCount() > 0
                || context.getTlsCertificateSdsSecretConfigsCount() > 0
                || context.hasTlsCertificateCertificateProvider()
                || context.hasValidationContextSdsSecretConfig()
                || context.hasValidationContextCertificateProvider()
                || context.hasCustomHandshaker()) {
            throw new IllegalArgumentException("Only bootstrap certificate providers are supported");
        }
        String identity = context.hasTlsCertificateProviderInstance()
                ? context.getTlsCertificateProviderInstance().getInstanceName()
                : null;
        if (context.hasTlsCertificateCertificateProviderInstance()) {
            identity = same(
                    identity,
                    context.getTlsCertificateCertificateProviderInstance().getInstanceName());
            if (context.hasTlsCertificateProviderInstance()
                    && !context.getTlsCertificateProviderInstance()
                            .getCertificateName()
                            .equals(context.getTlsCertificateCertificateProviderInstance()
                                    .getCertificateName())) {
                throw new IllegalArgumentException("Conflicting identity certificate names");
            }
        }
        CertificateValidationContext validation = CertificateValidationContext.getDefaultInstance();
        String root = null;
        String rootCertificateName = null;
        if (context.hasCombinedValidationContext()) {
            CommonTlsContext.CombinedCertificateValidationContext combined = context.getCombinedValidationContext();
            SecurityFields.only(
                    combined, "default_validation_context", "validation_context_certificate_provider_instance");
            if (combined.hasValidationContextSdsSecretConfig() || combined.hasValidationContextCertificateProvider()) {
                throw new IllegalArgumentException("Unsupported combined validation provider");
            }
            validation = combined.getDefaultValidationContext();
            if (combined.hasValidationContextCertificateProviderInstance()) {
                root = combined.getValidationContextCertificateProviderInstance()
                        .getInstanceName();
                rootCertificateName = combined.getValidationContextCertificateProviderInstance()
                        .getCertificateName();
            }
        } else if (context.hasValidationContext()) {
            validation = context.getValidationContext();
        } else if (context.hasValidationContextCertificateProviderInstance()) {
            root = context.getValidationContextCertificateProviderInstance().getInstanceName();
            rootCertificateName =
                    context.getValidationContextCertificateProviderInstance().getCertificateName();
        }
        if (validation.hasCaCertificateProviderInstance()) {
            root = same(root, validation.getCaCertificateProviderInstance().getInstanceName());
            same(
                    rootCertificateName,
                    validation.getCaCertificateProviderInstance().getCertificateName());
        }
        SecurityFields.only(
                validation,
                "ca_certificate_provider_instance",
                "match_subject_alt_names",
                "match_typed_subject_alt_names");
        if (validation.hasTrustedCa()
                || validation.hasSystemRootCerts()
                || validation.hasCustomValidatorConfig()
                || validation.hasCrl()
                || validation.getAllowExpiredCertificate()
                || validation.getTrustChainVerification()
                        != CertificateValidationContext.TrustChainVerification.VERIFY_TRUST_CHAIN
                || validation.getVerifyCertificateHashCount() > 0
                || validation.getVerifyCertificateSpkiCount() > 0) {
            throw new IllegalArgumentException("Unsupported certificate validation configuration");
        }
        for (StringMatcher matcher : validation.getMatchSubjectAltNamesList()) {
            XdsMatchers.string(matcher);
        }
        for (SubjectAltNameMatcher matcher : validation.getMatchTypedSubjectAltNamesList()) {
            SecurityFields.only(matcher, "san_type", "matcher");
            if (matcher.getSanType() == SubjectAltNameMatcher.SanType.SAN_TYPE_UNSPECIFIED
                    || matcher.getSanType() == SubjectAltNameMatcher.SanType.UNRECOGNIZED
                    || matcher.getSanType() == SubjectAltNameMatcher.SanType.OTHER_NAME) {
                throw new IllegalArgumentException("Invalid typed SAN matcher");
            }
            XdsMatchers.string(matcher.getMatcher());
        }
        provider(bootstrap, identity, true);
        provider(bootstrap, root, false);
        return new TlsPolicy(true, requireClient, identity, root, validation, serverName);
    }

    private static String same(String modern, String deprecated) {
        if (modern != null && !modern.equals(deprecated)) {
            throw new IllegalArgumentException("Conflicting certificate-provider fields");
        }
        return deprecated;
    }

    private static void provider(BootstrapInfo bootstrap, String name, boolean identity) {
        CertificateProviderInfo provider = bootstrap.getCertificateProviders().get(name);
        if (name == null || name.isEmpty() || provider == null || !"file_watcher".equals(provider.getPlugin())) {
            throw new IllegalArgumentException("TLS requires a known file_watcher certificate provider");
        }
        if (!provider.getConfig().containsFields(identity ? "certificate_file" : "ca_certificate_file")) {
            throw new IllegalArgumentException("Certificate provider lacks required certificate material");
        }
    }

    public boolean isTls() {
        return tls;
    }

    public boolean isClientCertificateRequired() {
        return requireClientCertificate;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public String getRootProvider() {
        return rootProvider;
    }

    public CertificateValidationContext getValidation() {
        return validation;
    }

    public String getServerName() {
        return serverName;
    }
}
