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

public class IstioConstant {
    /**
     * Address of the spiffe certificate provider. Defaults to discoveryAddress
     */
    public final static String CA_ADDR_KEY = "CA_ADDR";

    /**
     * CA and xDS services
     */
    public final static String DEFAULT_CA_ADDR = "istiod.istio-system.svc:15012";

    /**
     * The trust domain for spiffe certificates
     */
    public final static String TRUST_DOMAIN_KEY = "TRUST_DOMAIN";

    /**
     * The trust domain for spiffe certificates default value
     */
    public final static String DEFAULT_TRUST_DOMAIN = "cluster.local";

    public final static String WORKLOAD_NAMESPACE_KEY = "WORKLOAD_NAMESPACE";

    public final static String DEFAULT_WORKLOAD_NAMESPACE = "default";

    /**
     * k8s jwt token
     */
    public final static String KUBERNETES_SA_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";

    public final static String KUBERNETES_CA_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";

    public final static String ISTIO_SA_PATH = "/var/run/secrets/tokens/istio-token";

    public final static String ISTIO_CA_PATH = "/var/run/secrets/istio/root-cert.pem";

    public final static String KUBERNETES_NAMESPACE_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";

    public final static String RSA_KEY_SIZE_KEY = "RSA_KEY_SIZE";

    public final static String DEFAULT_RSA_KEY_SIZE = "2048";

    /**
     * The type of ECC signature algorithm to use when generating private keys
     */
    public final static String ECC_SIG_ALG_KEY = "ECC_SIGNATURE_ALGORITHM";

    public final static String DEFAULT_ECC_SIG_ALG = "ECDSA";

    /**
     * The cert lifetime requested by istio agent
     */
    public final static String SECRET_TTL_KEY = "SECRET_TTL";

    /**
     * The cert lifetime default value 24h0m0s
     */
    public final static String DEFAULT_SECRET_TTL = "86400"; //24 * 60 * 60

    /**
     * The grace period ratio for the cert rotation
     */
    public final static String SECRET_GRACE_PERIOD_RATIO_KEY = "SECRET_GRACE_PERIOD_RATIO";

    /**
     * The grace period ratio for the cert rotation, by default 0.5
     */
    public final static String DEFAULT_SECRET_GRACE_PERIOD_RATIO = "0.5";

    public final static String ISTIO_META_CLUSTER_ID_KEY = "ISTIO_META_CLUSTER_ID";

    public final static String PILOT_CERT_PROVIDER_KEY = "PILOT_CERT_PROVIDER";

    public final static String ISTIO_PILOT_CERT_PROVIDER = "istiod";

    public final static String DEFAULT_ISTIO_META_CLUSTER_ID = "Kubernetes";

    public final static String SPIFFE = "spiffe://";

    public final static String NS = "/ns/";

    public final static String SA = "/sa/";

    public final static String JWT_POLICY = "JWT_POLICY";

    public final static String DEFAULT_JWT_POLICY = "first-party-jwt";

    public final static String FIRST_PARTY_JWT = "first-party-jwt";

    public final static String THIRD_PARTY_JWT = "third-party-jwt";

}
