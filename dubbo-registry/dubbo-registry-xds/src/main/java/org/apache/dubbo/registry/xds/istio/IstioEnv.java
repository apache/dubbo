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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.xds.XdsEnv;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class IstioEnv implements XdsEnv {
    private static final Logger logger = LoggerFactory.getLogger(IstioEnv.class);

    private String caAddr;

    private String serviceAccount = null;

    private String csrHost;

    private String trustDomain;

    private String workloadNameSpace;

    private int rasKeySize;

    private String eccSigAlg;

    private int secretTTL;

    private float secretGracePeriodRatio;

    private String istioMetaClusterId;

    public IstioEnv() {
        File saFile = new File(IstioConstant.KUBERNETES_SA_PATH);
        if (saFile.canRead()) {
            try {
                serviceAccount = FileUtils.readFileToString(saFile, StandardCharsets.UTF_8);
                trustDomain = Optional.ofNullable(System.getenv(IstioConstant.TRUST_DOMAIN_KEY)).orElse(IstioConstant.DEFAULT_TRUST_DOMAIN);
                workloadNameSpace = Optional.ofNullable(System.getenv(IstioConstant.WORKLOAD_NAMESPACE_KEY)).orElse(IstioConstant.DEFAULT_WORKLOAD_NAMESPACE);
                csrHost = "spiffe://" + trustDomain + "/ns/" + workloadNameSpace + "/sa/" + serviceAccount;
                caAddr = Optional.ofNullable(System.getenv(IstioConstant.CA_ADDR_KEY)).orElse(IstioConstant.DEFAULT_CA_ADDR);
                rasKeySize = Integer.parseInt(Optional.ofNullable(System.getenv(IstioConstant.RSA_KEY_SIZE_KEY)).orElse(IstioConstant.DEFAULT_RSA_KEY_SIZE));
                eccSigAlg = Optional.ofNullable(System.getenv(IstioConstant.ECC_SIG_ALG_KEY)).orElse(IstioConstant.DEFAULT_ECC_SIG_ALG);
                secretTTL = Integer.parseInt(Optional.ofNullable(System.getenv(IstioConstant.SECRET_TTL_KEY)).orElse(IstioConstant.DEFAULT_SECRET_TTL));
                secretGracePeriodRatio = Float.parseFloat(Optional.ofNullable(System.getenv(IstioConstant.SECRET_GRACE_PERIOD_RATIO_KEY)).orElse(IstioConstant.DEFAULT_SECRET_GRACE_PERIOD_RATIO));
                istioMetaClusterId = Optional.ofNullable(System.getenv(IstioConstant.ISTIO_META_CLUSTER_ID_KEY)).orElse(IstioConstant.DEFAULT_ISTIO_META_CLUSTER_ID);
            } catch (IOException e) {
                logger.error("Unable to read token file.", e);
            }
        }
        if (serviceAccount == null) {
            throw new UnsupportedOperationException("Unable to found kubernetes service account token file. " +
                    "Please check if work in Kubernetes and mount service account token file correctly.");
        }
    }

    public String getCaAddr() {
        return caAddr;
    }

    public String getServiceAccount() {
        return serviceAccount;
    }

    public String getCsrHost() {
        return csrHost;
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
        return "ECDSA".equals(eccSigAlg);
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
}
