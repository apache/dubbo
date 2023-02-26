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
package org.apache.dubbo.registry.xds.istio;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.xds.XdsEnv;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_READ_FILE_ISTIO;
import static org.apache.dubbo.registry.xds.istio.IstioConstant.NS;
import static org.apache.dubbo.registry.xds.istio.IstioConstant.SA;
import static org.apache.dubbo.registry.xds.istio.IstioConstant.SPIFFE;

public class IstioEnv implements XdsEnv {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(IstioEnv.class);

    private static final IstioEnv INSTANCE = new IstioEnv();

    private String podName;

    private String caAddr;

    private String jwtPolicy;

    private String trustDomain;

    private String workloadNameSpace;

    private int rasKeySize;

    private String eccSigAlg;

    private int secretTTL;

    private float secretGracePeriodRatio;

    private String istioMetaClusterId;

    private String pilotCertProvider;

    private IstioEnv() {
        jwtPolicy = Optional.ofNullable(System.getenv(IstioConstant.JWT_POLICY)).orElse(IstioConstant.DEFAULT_JWT_POLICY);
        podName = Optional.ofNullable(System.getenv("POD_NAME")).orElse(System.getenv("HOSTNAME"));
        trustDomain = Optional.ofNullable(System.getenv(IstioConstant.TRUST_DOMAIN_KEY)).orElse(IstioConstant.DEFAULT_TRUST_DOMAIN);
        workloadNameSpace = Optional.ofNullable(System.getenv(IstioConstant.WORKLOAD_NAMESPACE_KEY))
            .orElseGet(()->{
                File namespaceFile = new File(IstioConstant.KUBERNETES_NAMESPACE_PATH);
                if (namespaceFile.canRead()) {
                    try {
                        return FileUtils.readFileToString(namespaceFile, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        logger.error(REGISTRY_ERROR_READ_FILE_ISTIO,  "", "", "read namespace file error", e);
                    }
                }
                return IstioConstant.DEFAULT_WORKLOAD_NAMESPACE;
            });
        caAddr = Optional.ofNullable(System.getenv(IstioConstant.CA_ADDR_KEY)).orElse(IstioConstant.DEFAULT_CA_ADDR);
        rasKeySize = Integer.parseInt(Optional.ofNullable(System.getenv(IstioConstant.RSA_KEY_SIZE_KEY)).orElse(IstioConstant.DEFAULT_RSA_KEY_SIZE));
        eccSigAlg = Optional.ofNullable(System.getenv(IstioConstant.ECC_SIG_ALG_KEY)).orElse(IstioConstant.DEFAULT_ECC_SIG_ALG);
        secretTTL = Integer.parseInt(Optional.ofNullable(System.getenv(IstioConstant.SECRET_TTL_KEY)).orElse(IstioConstant.DEFAULT_SECRET_TTL));
        secretGracePeriodRatio = Float.parseFloat(Optional.ofNullable(System.getenv(IstioConstant.SECRET_GRACE_PERIOD_RATIO_KEY)).orElse(IstioConstant.DEFAULT_SECRET_GRACE_PERIOD_RATIO));
        istioMetaClusterId = Optional.ofNullable(System.getenv(IstioConstant.ISTIO_META_CLUSTER_ID_KEY)).orElse(IstioConstant.DEFAULT_ISTIO_META_CLUSTER_ID);
        pilotCertProvider = Optional.ofNullable(System.getenv(IstioConstant.PILOT_CERT_PROVIDER_KEY)).orElse("");

        if (getServiceAccount() == null) {
            throw new UnsupportedOperationException("Unable to found kubernetes service account token file. " +
                "Please check if work in Kubernetes and mount service account token file correctly.");
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
                logger.error(LoggerCodeConstants.REGISTRY_ISTIO_EXCEPTION, "File Read Failed", "", "Unable to read token file.", e);
            }
        }

        return null;
    }

    public String getCsrHost() {
        // spiffe://<trust_domain>/ns/<namespace>/sa/<service_account>
        return SPIFFE + trustDomain + NS + workloadNameSpace + SA + getServiceAccount();
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
        return IstioConstant.DEFAULT_ECC_SIG_ALG.equals(eccSigAlg);
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

    public String getCaCert() {
        File caFile;
        if (IstioConstant.ISTIO_PILOT_CERT_PROVIDER.equals(pilotCertProvider)) {
            caFile = new File(IstioConstant.ISTIO_CA_PATH);
        } else {
            return null;
        }
        if (caFile.canRead()) {
            try {
                return FileUtils.readFileToString(caFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error(LoggerCodeConstants.REGISTRY_ISTIO_EXCEPTION, "File Read Failed", "", "read ca file error", e);
            }
        }
        return null;
    }
}
