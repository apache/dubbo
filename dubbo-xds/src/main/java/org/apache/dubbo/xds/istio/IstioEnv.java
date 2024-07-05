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
package org.apache.dubbo.xds.istio;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.io.FileUtils;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_READ_FILE_ISTIO;
import static org.apache.dubbo.xds.istio.IstioConstant.CA_ADDR_KEY;
import static org.apache.dubbo.xds.istio.IstioConstant.DEFAULT_CA_ADDR;
import static org.apache.dubbo.xds.istio.IstioConstant.DEFAULT_ECC_SIG_ALG;
import static org.apache.dubbo.xds.istio.IstioConstant.DEFAULT_ISTIO_META_CLUSTER_ID;
import static org.apache.dubbo.xds.istio.IstioConstant.DEFAULT_JWT_POLICY;
import static org.apache.dubbo.xds.istio.IstioConstant.DEFAULT_RSA_KEY_SIZE;
import static org.apache.dubbo.xds.istio.IstioConstant.DEFAULT_SECRET_TTL;
import static org.apache.dubbo.xds.istio.IstioConstant.DEFAULT_TRUST_DOMAIN;
import static org.apache.dubbo.xds.istio.IstioConstant.DEFAULT_TRUST_TTL;
import static org.apache.dubbo.xds.istio.IstioConstant.ECC_SIG_ALG_KEY;
import static org.apache.dubbo.xds.istio.IstioConstant.ISTIO_META_CLUSTER_ID_KEY;
import static org.apache.dubbo.xds.istio.IstioConstant.JWT_POLICY;
import static org.apache.dubbo.xds.istio.IstioConstant.NS;
import static org.apache.dubbo.xds.istio.IstioConstant.RSA_KEY_SIZE_KEY;
import static org.apache.dubbo.xds.istio.IstioConstant.SA;
import static org.apache.dubbo.xds.istio.IstioConstant.SECRET_TTL_KEY;
import static org.apache.dubbo.xds.istio.IstioConstant.SPIFFE;
import static org.apache.dubbo.xds.istio.IstioConstant.TRUST_DOMAIN_KEY;
import static org.apache.dubbo.xds.istio.IstioConstant.TRUST_TTL_KEY;

public class IstioEnv implements XdsEnv {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(IstioEnv.class);

    private static final IstioEnv INSTANCE = new IstioEnv();

    /**
     * TODO this can auto read from sa jwt
     */
    private String podName;

    private String caAddr;

    private String jwtPolicy;

    private String trustDomain;

    /**
     * TODO this can auto read from sa jwt
     */
    private String workloadNameSpace;

    private int rasKeySize;

    private String eccSigAlg;

    private float secretGracePeriodRatio;

    private String istioMetaClusterId;

    /**
     * Who provides cert for istio pilot
     */
    private String pilotCertProvider;

    /**
     * TTL of cert pair. This will affect the frequency of cert refresh.
     */
    private int secretTTL;

    /**
     * The time start to try to refresh certs
     */
    private long tryRefreshBeforeCertExpireAt;

    /**
     * TTL of trust storage. This will affect the frequency of trust refresh.
     * In istio, trust always refresh with cert pair
     * because istio use cert chains as response for an CSR request.
     */
    private long trustTTL;

    private String serviceAccountJwt;

    /**
     * TODO this can auto read from sa jwt
     */
    private String serviceAccountName;

    private boolean haveServiceAccount;

    private IstioEnv() {
        jwtPolicy = getStringProp(JWT_POLICY, DEFAULT_JWT_POLICY);
        podName = Optional.ofNullable(getStringProp("POD_NAME", (String) null)).orElse(getStringProp("HOSTNAME", ""));
        trustDomain = getStringProp(TRUST_DOMAIN_KEY, DEFAULT_TRUST_DOMAIN);

        workloadNameSpace = getStringProp(IstioConstant.WORKLOAD_NAMESPACE_KEY, () -> {
            File namespaceFile = new File(IstioConstant.KUBERNETES_NAMESPACE_PATH);
            if (namespaceFile.canRead()) {
                try {
                    return FileUtils.readFileToString(namespaceFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    logger.error(REGISTRY_ERROR_READ_FILE_ISTIO, "", "", "read namespace file error", e);
                }
            }
            return IstioConstant.DEFAULT_WORKLOAD_NAMESPACE;
        });
        caAddr = getStringProp(CA_ADDR_KEY, DEFAULT_CA_ADDR);

        rasKeySize = getIntProp(RSA_KEY_SIZE_KEY, DEFAULT_RSA_KEY_SIZE);
        eccSigAlg = getStringProp(ECC_SIG_ALG_KEY, DEFAULT_ECC_SIG_ALG);
        secretTTL = getIntProp(SECRET_TTL_KEY, DEFAULT_SECRET_TTL);
        trustTTL = getIntProp(TRUST_TTL_KEY, DEFAULT_TRUST_TTL);

        secretGracePeriodRatio =
                Float.parseFloat(Optional.ofNullable(System.getenv(IstioConstant.SECRET_GRACE_PERIOD_RATIO_KEY))
                        .orElse(IstioConstant.DEFAULT_SECRET_GRACE_PERIOD_RATIO));
        istioMetaClusterId = getStringProp(ISTIO_META_CLUSTER_ID_KEY, DEFAULT_ISTIO_META_CLUSTER_ID);
        pilotCertProvider = getStringProp(IstioConstant.PILOT_CERT_PROVIDER_KEY, "");
        serviceAccountName = getStringProp(IstioConstant.SERVICE_NAME_KEY, "default");
        if (getServiceAccount() == null) {
            haveServiceAccount = false;
            logger.info("Unable to found kubernetes service account token. Some istio-XDS feature may disabled.");
        }
    }

    public static IstioEnv getInstance() {
        return INSTANCE;
    }

    public String getPodName() {
        return podName;
    }

    public String getCaAddr() {
        return caAddr;
    }

    public String getServiceAccount() {
        File saFile;
        switch (jwtPolicy) {
            case IstioConstant.FIRST_PARTY_JWT:
                saFile = new File(IstioConstant.KUBERNETES_SA_PATH);
                break;
            case IstioConstant.THIRD_PARTY_JWT:
            default:
                saFile = new File(IstioConstant.ISTIO_SA_PATH);
        }
        if (saFile.canRead()) {
            try {
                return FileUtils.readFileToString(saFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error(
                        LoggerCodeConstants.REGISTRY_ISTIO_EXCEPTION,
                        "File Read Failed",
                        "",
                        "Unable to read token file.",
                        e);
            }
        }

        return null;
    }

    public String getServiceAccountJwt() {
        return serviceAccountJwt;
    }

    public String getCsrHost() {
        // spiffe://<trust_domain>/ns/<namespace>/sa/<service_account>
        return SPIFFE + trustDomain + NS + workloadNameSpace + SA + getServiceAccountName();
    }

    public String getIstioMetaNamespace() {
        return getCsrHost();
    }

    public String getTrustDomain() {
        return trustDomain;
    }

    public String getWorkloadNameSpace() {
        return workloadNameSpace;
    }

    @Override
    public String getCluster() {
        return null;
    }

    public int getRasKeySize() {
        return rasKeySize;
    }

    public boolean isECCFirst() {
        return DEFAULT_ECC_SIG_ALG.equals(eccSigAlg);
    }

    public int getSecretTTL() {
        return secretTTL;
    }

    public float getSecretGracePeriodRatio() {
        return secretGracePeriodRatio;
    }

    public String getIstioMetaClusterId() {
        return istioMetaClusterId;
    }

    public Long getTryRefreshBeforeCertExpireAt() {
        return tryRefreshBeforeCertExpireAt;
    }

    public String getPilotCertProvider() {
        return pilotCertProvider;
    }

    public long getTrustTTL() {
        return trustTTL;
    }

    public String getServiceAccountName() {
        return serviceAccountName;
    }

    // for test
    @Deprecated
    public void setToken(String saJwtToken) {
        serviceAccountJwt = saJwtToken;
    }

    public String getCaCert() {
        File caFile;
        if (IstioConstant.PILOT_CERT_PROVIDER_ISTIO.equals(pilotCertProvider)) {
            caFile = new File(IstioConstant.ISTIO_CA_PATH);
        } else {
            return null;
        }
        if (caFile.canRead()) {
            try {
                return FileUtils.readFileToString(caFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error(
                        LoggerCodeConstants.REGISTRY_ISTIO_EXCEPTION, "File Read Failed", "", "read ca file error", e);
            }
        }
        return null;
    }

    public boolean haveServiceAccount() {
        return haveServiceAccount;
    }
}
