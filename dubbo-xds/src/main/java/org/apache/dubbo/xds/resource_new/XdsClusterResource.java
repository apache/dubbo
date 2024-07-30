/*
 * Copyright 2022 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource_new;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;
import org.apache.dubbo.xds.resource_new.cluster.LoadBalancerConfigFactory;
import org.apache.dubbo.xds.resource_new.cluster.OutlierDetection;
import org.apache.dubbo.xds.resource_new.listener.security.UpstreamTlsContext;
import org.apache.dubbo.xds.resource_new.exception.ResourceInvalidException;
import org.apache.dubbo.xds.resource_new.update.CdsUpdate;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.Duration;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;
import io.envoyproxy.envoy.config.cluster.v3.CircuitBreakers.Thresholds;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.core.v3.RoutingPriority;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CertificateValidationContext;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;

public class XdsClusterResource extends XdsResourceType<CdsUpdate> {
    static final String ADS_TYPE_URL_CDS = "type.googleapis.com/envoy.config.cluster.v3.Cluster";
    private static final String TYPE_URL_UPSTREAM_TLS_CONTEXT = "type.googleapis.com/envoy.extensions"
            + ".transport_sockets.tls.v3.UpstreamTlsContext";
    private static final String TYPE_URL_UPSTREAM_TLS_CONTEXT_V2 = "type.googleapis.com/envoy.api.v2.auth"
            + ".UpstreamTlsContext";

    private static final XdsClusterResource instance = new XdsClusterResource();

    public static XdsClusterResource getInstance() {
        return instance;
    }

    @Override
    @Nullable
    String extractResourceName(Message unpackedResource) {
        if (!(unpackedResource instanceof Cluster)) {
            return null;
        }
        return ((Cluster) unpackedResource).getName();
    }

    @Override
    String typeName() {
        return "CDS";
    }

    @Override
    String typeUrl() {
        return ADS_TYPE_URL_CDS;
    }

    @Override
    boolean isFullStateOfTheWorld() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    Class<Cluster> unpackedClassName() {
        return Cluster.class;
    }

    @Override
    CdsUpdate doParse(Args args, Message unpackedMessage) throws ResourceInvalidException {
        if (!(unpackedMessage instanceof Cluster)) {
            throw new ResourceInvalidException("Invalid message type: " + unpackedMessage.getClass());
        }
        Set<String> certProviderInstances = null;
        if (args.bootstrapInfo != null && args.bootstrapInfo.certProviders() != null) {
            certProviderInstances = args.bootstrapInfo.certProviders()
                    .keySet();
        }
        return processCluster((Cluster) unpackedMessage, certProviderInstances, args.serverInfo);
    }

    static CdsUpdate processCluster(
            Cluster cluster, Set<String> certProviderInstances, ServerInfo serverInfo) throws ResourceInvalidException {
        StructOrError<CdsUpdate.Builder> structOrError;
        switch (cluster.getClusterDiscoveryTypeCase()) {
            case TYPE:
                structOrError = parseNonAggregateCluster(cluster, certProviderInstances, serverInfo);
                break;
            case CLUSTER_TYPE:
                structOrError = parseAggregateCluster(cluster);
                break;
            case CLUSTERDISCOVERYTYPE_NOT_SET:
            default:
                throw new ResourceInvalidException(
                        "Cluster " + cluster.getName() + ": unspecified cluster discovery type");
        }
        if (structOrError.getErrorDetail() != null) {
            throw new ResourceInvalidException(structOrError.getErrorDetail());
        }
        CdsUpdate.Builder updateBuilder = structOrError.getStruct();

        Map<String, ?> lbPolicyConfig = LoadBalancerConfigFactory.newConfig(cluster, enableLeastRequest, enableWrr,
                enablePickFirst);

        updateBuilder.lbPolicyConfig(lbPolicyConfig);

        return updateBuilder.build();
    }

    private static StructOrError<CdsUpdate.Builder> parseAggregateCluster(Cluster cluster) {
        String clusterName = cluster.getName();
        Cluster.CustomClusterType customType = cluster.getClusterType();
        String typeName = customType.getName();
        if (!typeName.equals(AGGREGATE_CLUSTER_TYPE_NAME)) {
            return StructOrError.fromError("Cluster " + clusterName + ": unsupported custom cluster type: " + typeName);
        }
        io.envoyproxy.envoy.extensions.clusters.aggregate.v3.ClusterConfig clusterConfig;
        try {
            clusterConfig = unpackCompatibleType(customType.getTypedConfig(),
                    io.envoyproxy.envoy.extensions.clusters.aggregate.v3.ClusterConfig.class, TYPE_URL_CLUSTER_CONFIG
                    , null);
        } catch (InvalidProtocolBufferException e) {
            return StructOrError.fromError("Cluster " + clusterName + ": malformed ClusterConfig: " + e);
        }
        return StructOrError.fromStruct(CdsUpdate.forAggregate(clusterName, clusterConfig.getClustersList()));
    }

    private static StructOrError<CdsUpdate.Builder> parseNonAggregateCluster(
            Cluster cluster, Set<String> certProviderInstances, ServerInfo serverInfo) {
        String clusterName = cluster.getName();
        ServerInfo lrsServerInfo = null;
        Long maxConcurrentRequests = null;
        UpstreamTlsContext upstreamTlsContext = null;
        OutlierDetection outlierDetection = null;
        if (cluster.hasLrsServer()) {
            if (!cluster.getLrsServer()
                    .hasSelf()) {
                return StructOrError.fromError(
                        "Cluster " + clusterName + ": only support LRS for the same management server");
            }
            lrsServerInfo = serverInfo;
        }
        if (cluster.hasCircuitBreakers()) {
            List<Thresholds> thresholds = cluster.getCircuitBreakers()
                    .getThresholdsList();
            for (Thresholds threshold : thresholds) {
                if (threshold.getPriority() != RoutingPriority.DEFAULT) {
                    continue;
                }
                if (threshold.hasMaxRequests()) {
                    maxConcurrentRequests = (long) threshold.getMaxRequests()
                            .getValue();
                }
            }
        }
        if (cluster.getTransportSocketMatchesCount() > 0) {
            return StructOrError.fromError("Cluster " + clusterName + ": transport-socket-matches not supported.");
        }
        if (cluster.hasTransportSocket()) {
            if (!TRANSPORT_SOCKET_NAME_TLS.equals(cluster.getTransportSocket()
                    .getName())) {
                return StructOrError.fromError("transport-socket with name " + cluster.getTransportSocket()
                        .getName() + " not supported.");
            }
            try {
                upstreamTlsContext =
                        UpstreamTlsContext.fromEnvoyProtoUpstreamTlsContext(validateUpstreamTlsContext(unpackCompatibleType(cluster.getTransportSocket()
                        .getTypedConfig(),
                                io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext.class,
                                TYPE_URL_UPSTREAM_TLS_CONTEXT, TYPE_URL_UPSTREAM_TLS_CONTEXT_V2),
                                certProviderInstances));
            } catch (InvalidProtocolBufferException | ResourceInvalidException e) {
                return StructOrError.fromError("Cluster " + clusterName + ": malformed UpstreamTlsContext: " + e);
            }
        }

        if (cluster.hasOutlierDetection()) {
            try {
                outlierDetection =
                        OutlierDetection.fromEnvoyOutlierDetection(validateOutlierDetection(cluster.getOutlierDetection()));
            } catch (ResourceInvalidException e) {
                return StructOrError.fromError("Cluster " + clusterName + ": malformed outlier_detection: " + e);
            }
        }

        Cluster.DiscoveryType type = cluster.getType();
        if (type == Cluster.DiscoveryType.EDS) {
            String edsServiceName = null;
            Cluster.EdsClusterConfig edsClusterConfig = cluster.getEdsClusterConfig();
            if (!edsClusterConfig.getEdsConfig()
                    .hasAds() && !edsClusterConfig.getEdsConfig()
                    .hasSelf()) {
                return StructOrError.fromError(
                        "Cluster " + clusterName + ": field eds_cluster_config must be set to indicate to use"
                                + " EDS over ADS or self ConfigSource");
            }
            // If the service_name field is set, that value will be used for the EDS request.
            if (!edsClusterConfig.getServiceName()
                    .isEmpty()) {
                edsServiceName = edsClusterConfig.getServiceName();
            }
            // edsServiceName is required if the CDS resource has an xdstp name.
            if ((edsServiceName == null) && clusterName.toLowerCase()
                    .startsWith("xdstp:")) {
                return StructOrError.fromError("EDS service_name must be set when Cluster resource has an xdstp name");
            }
            return StructOrError.fromStruct(CdsUpdate.forEds(clusterName, edsServiceName, lrsServerInfo,
                    maxConcurrentRequests, upstreamTlsContext, outlierDetection));
        } else if (type.equals(Cluster.DiscoveryType.LOGICAL_DNS)) {
            if (!cluster.hasLoadAssignment()) {
                return StructOrError.fromError(
                        "Cluster " + clusterName + ": LOGICAL_DNS clusters must have a single host");
            }
            ClusterLoadAssignment assignment = cluster.getLoadAssignment();
            if (assignment.getEndpointsCount() != 1 || assignment.getEndpoints(0)
                    .getLbEndpointsCount() != 1) {
                return StructOrError.fromError("Cluster " + clusterName + ": LOGICAL_DNS clusters must have a single "
                        + "locality_lb_endpoint and a single lb_endpoint");
            }
            io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint lbEndpoint = assignment.getEndpoints(0)
                    .getLbEndpoints(0);
            if (!lbEndpoint.hasEndpoint() || !lbEndpoint.getEndpoint()
                    .hasAddress() || !lbEndpoint.getEndpoint()
                    .getAddress()
                    .hasSocketAddress()) {
                return StructOrError.fromError("Cluster " + clusterName
                        + ": LOGICAL_DNS clusters must have an endpoint with address and socket_address");
            }
            SocketAddress socketAddress = lbEndpoint.getEndpoint()
                    .getAddress()
                    .getSocketAddress();
            if (!socketAddress.getResolverName()
                    .isEmpty()) {
                return StructOrError.fromError(
                        "Cluster " + clusterName + ": LOGICAL DNS clusters must NOT have a custom resolver name set");
            }
            if (socketAddress.getPortSpecifierCase() != SocketAddress.PortSpecifierCase.PORT_VALUE) {
                return StructOrError.fromError(
                        "Cluster " + clusterName + ": LOGICAL DNS clusters socket_address must have port_value");
            }
            String dnsHostName = String.format(Locale.US, "%s:%d", socketAddress.getAddress(),
                    socketAddress.getPortValue());
            return StructOrError.fromStruct(CdsUpdate.forLogicalDns(clusterName, dnsHostName, lrsServerInfo,
                    maxConcurrentRequests, upstreamTlsContext));
        }
        return StructOrError.fromError("Cluster " + clusterName + ": unsupported built-in discovery type: " + type);
    }

    static io.envoyproxy.envoy.config.cluster.v3.OutlierDetection validateOutlierDetection(
            io.envoyproxy.envoy.config.cluster.v3.OutlierDetection outlierDetection) throws ResourceInvalidException {
        if (outlierDetection.hasInterval()) {
            if (!Durations.isValid(outlierDetection.getInterval())) {
                throw new ResourceInvalidException("outlier_detection interval is not a valid Duration");
            }
            if (hasNegativeValues(outlierDetection.getInterval())) {
                throw new ResourceInvalidException("outlier_detection interval has a negative value");
            }
        }
        if (outlierDetection.hasBaseEjectionTime()) {
            if (!Durations.isValid(outlierDetection.getBaseEjectionTime())) {
                throw new ResourceInvalidException("outlier_detection base_ejection_time is not a valid Duration");
            }
            if (hasNegativeValues(outlierDetection.getBaseEjectionTime())) {
                throw new ResourceInvalidException("outlier_detection base_ejection_time has a negative value");
            }
        }
        if (outlierDetection.hasMaxEjectionTime()) {
            if (!Durations.isValid(outlierDetection.getMaxEjectionTime())) {
                throw new ResourceInvalidException("outlier_detection max_ejection_time is not a valid Duration");
            }
            if (hasNegativeValues(outlierDetection.getMaxEjectionTime())) {
                throw new ResourceInvalidException("outlier_detection max_ejection_time has a negative value");
            }
        }
        if (outlierDetection.hasMaxEjectionPercent() && outlierDetection.getMaxEjectionPercent()
                .getValue() > 100) {
            throw new ResourceInvalidException("outlier_detection max_ejection_percent is > 100");
        }
        if (outlierDetection.hasEnforcingSuccessRate() && outlierDetection.getEnforcingSuccessRate()
                .getValue() > 100) {
            throw new ResourceInvalidException("outlier_detection enforcing_success_rate is > 100");
        }
        if (outlierDetection.hasFailurePercentageThreshold() && outlierDetection.getFailurePercentageThreshold()
                .getValue() > 100) {
            throw new ResourceInvalidException("outlier_detection failure_percentage_threshold is > 100");
        }
        if (outlierDetection.hasEnforcingFailurePercentage() && outlierDetection.getEnforcingFailurePercentage()
                .getValue() > 100) {
            throw new ResourceInvalidException("outlier_detection enforcing_failure_percentage is > 100");
        }

        return outlierDetection;
    }

    static boolean hasNegativeValues(Duration duration) {
        return duration.getSeconds() < 0 || duration.getNanos() < 0;
    }

    public static io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext validateUpstreamTlsContext(
            io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext upstreamTlsContext,
            Set<String> certProviderInstances) throws ResourceInvalidException {
        if (upstreamTlsContext.hasCommonTlsContext()) {
            validateCommonTlsContext(upstreamTlsContext.getCommonTlsContext(), certProviderInstances, false);
        } else {
            throw new ResourceInvalidException("common-tls-context is required in upstream-tls-context");
        }
        return upstreamTlsContext;
    }

    static void validateCommonTlsContext(
            CommonTlsContext commonTlsContext,
            Set<String> certProviderInstances,
            boolean server) throws ResourceInvalidException {
        if (commonTlsContext.hasCustomHandshaker()) {
            throw new ResourceInvalidException("common-tls-context with custom_handshaker is not supported");
        }
        if (commonTlsContext.hasTlsParams()) {
            throw new ResourceInvalidException("common-tls-context with tls_params is not supported");
        }
        if (commonTlsContext.hasValidationContextSdsSecretConfig()) {
            throw new ResourceInvalidException("common-tls-context with validation_context_sds_secret_config is not "
                    + "supported");
        }
        if (commonTlsContext.hasValidationContextCertificateProvider()) {
            throw new ResourceInvalidException("common-tls-context with validation_context_certificate_provider is "
                    + "not supported");
        }
        if (commonTlsContext.hasValidationContextCertificateProviderInstance()) {
            throw new ResourceInvalidException(
                    "common-tls-context with validation_context_certificate_provider_instance is not" + " supported");
        }
        String certInstanceName = getIdentityCertInstanceName(commonTlsContext);
        if (certInstanceName == null) {
            if (server) {
                throw new ResourceInvalidException("tls_certificate_provider_instance is required in "
                        + "downstream-tls-context");
            }
            if (commonTlsContext.getTlsCertificatesCount() > 0) {
                throw new ResourceInvalidException("tls_certificate_provider_instance is unset");
            }
            if (commonTlsContext.getTlsCertificateSdsSecretConfigsCount() > 0) {
                throw new ResourceInvalidException("tls_certificate_provider_instance is unset");
            }
            if (commonTlsContext.hasTlsCertificateCertificateProvider()) {
                throw new ResourceInvalidException("tls_certificate_provider_instance is unset");
            }
        } else if (certProviderInstances == null || !certProviderInstances.contains(certInstanceName)) {
            throw new ResourceInvalidException(
                    "CertificateProvider instance name '" + certInstanceName + "' not defined in the bootstrap file.");
        }
        String rootCaInstanceName = getRootCertInstanceName(commonTlsContext);
        if (rootCaInstanceName == null) {
            if (!server) {
                throw new ResourceInvalidException("ca_certificate_provider_instance is required in "
                        + "upstream-tls-context");
            }
        } else {
            if (certProviderInstances == null || !certProviderInstances.contains(rootCaInstanceName)) {
                throw new ResourceInvalidException("ca_certificate_provider_instance name '" + rootCaInstanceName
                        + "' not defined in the bootstrap file.");
            }
            CertificateValidationContext certificateValidationContext = null;
            if (commonTlsContext.hasValidationContext()) {
                certificateValidationContext = commonTlsContext.getValidationContext();
            } else if (commonTlsContext.hasCombinedValidationContext()
                    && commonTlsContext.getCombinedValidationContext()
                    .hasDefaultValidationContext()) {
                certificateValidationContext = commonTlsContext.getCombinedValidationContext()
                        .getDefaultValidationContext();
            }
            if (certificateValidationContext != null) {
                if (certificateValidationContext.getMatchSubjectAltNamesCount() > 0 && server) {
                    throw new ResourceInvalidException("match_subject_alt_names only allowed in upstream_tls_context");
                }
                if (certificateValidationContext.getVerifyCertificateSpkiCount() > 0) {
                    throw new ResourceInvalidException("verify_certificate_spki in default_validation_context is not "
                            + "supported");
                }
                if (certificateValidationContext.getVerifyCertificateHashCount() > 0) {
                    throw new ResourceInvalidException("verify_certificate_hash in default_validation_context is not "
                            + "supported");
                }
                if (certificateValidationContext.hasRequireSignedCertificateTimestamp()) {
                    throw new ResourceInvalidException(
                            "require_signed_certificate_timestamp in default_validation_context is not " + "supported");
                }
                if (certificateValidationContext.hasCrl()) {
                    throw new ResourceInvalidException("crl in default_validation_context is not supported");
                }
                if (certificateValidationContext.hasCustomValidatorConfig()) {
                    throw new ResourceInvalidException("custom_validator_config in default_validation_context is not "
                            + "supported");
                }
            }
        }
    }

    private static String getIdentityCertInstanceName(CommonTlsContext commonTlsContext) {
        if (commonTlsContext.hasTlsCertificateProviderInstance()) {
            return commonTlsContext.getTlsCertificateProviderInstance()
                    .getInstanceName();
        } else if (commonTlsContext.hasTlsCertificateCertificateProviderInstance()) {
            return commonTlsContext.getTlsCertificateCertificateProviderInstance()
                    .getInstanceName();
        }
        return null;
    }

    private static String getRootCertInstanceName(CommonTlsContext commonTlsContext) {
        if (commonTlsContext.hasValidationContext()) {
            if (commonTlsContext.getValidationContext()
                    .hasCaCertificateProviderInstance()) {
                return commonTlsContext.getValidationContext()
                        .getCaCertificateProviderInstance()
                        .getInstanceName();
            }
        } else if (commonTlsContext.hasCombinedValidationContext()) {
            CommonTlsContext.CombinedCertificateValidationContext combinedCertificateValidationContext =
                    commonTlsContext.getCombinedValidationContext();
            if (combinedCertificateValidationContext.hasDefaultValidationContext()
                    && combinedCertificateValidationContext.getDefaultValidationContext()
                    .hasCaCertificateProviderInstance()) {
                return combinedCertificateValidationContext.getDefaultValidationContext()
                        .getCaCertificateProviderInstance()
                        .getInstanceName();
            } else if (combinedCertificateValidationContext.hasValidationContextCertificateProviderInstance()) {
                return combinedCertificateValidationContext.getValidationContextCertificateProviderInstance()
                        .getInstanceName();
            }
        }
        return null;
    }

}
