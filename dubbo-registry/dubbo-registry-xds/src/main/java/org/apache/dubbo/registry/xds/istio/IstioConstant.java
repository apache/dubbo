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
    public final static String CA_ADDR_KEY = "CA_ADDR";

    public final static String DEFAULT_CA_ADDR = "istiod.istio-system.svc:15012";

    public final static String TRUST_DOMAIN_KEY = "TRUST_DOMAIN";

    public final static String DEFAULT_TRUST_DOMAIN = "cluster.local";

    public final static String WORKLOAD_NAMESPACE_KEY = "WORKLOAD_NAMESPACE";

    public final static String DEFAULT_WORKLOAD_NAMESPACE = "default";

    public final static String KUBERNETES_SA_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";

    public final static String RSA_KEY_SIZE_KEY = "RSA_KEY_SIZE";

    public final static String DEFAULT_RSA_KEY_SIZE = "2048";

    public final static String ECC_SIG_ALG_KEY = "ECC_SIGNATURE_ALGORITHM";

    public final static String DEFAULT_ECC_SIG_ALG = "ECDSA";

    public final static String SECRET_TTL_KEY = "SECRET_TTL";

    public final static String DEFAULT_SECRET_TTL = "86400"; //24 * 60 * 60

    public final static String SECRET_GRACE_PERIOD_RATIO_KEY = "SECRET_GRACE_PERIOD_RATIO";

    public final static String DEFAULT_SECRET_GRACE_PERIOD_RATIO = "0.5";

    public final static String ISTIO_META_CLUSTER_ID_KEY = "ISTIO_META_CLUSTER_ID";

    public final static String DEFAULT_ISTIO_META_CLUSTER_ID = "kubernetes";
}
