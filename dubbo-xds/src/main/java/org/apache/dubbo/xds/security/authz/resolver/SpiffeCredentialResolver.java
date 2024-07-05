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
package org.apache.dubbo.xds.security.authz.resolver;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.xds.security.authz.RequestCredential;
import org.apache.dubbo.xds.security.authz.resolver.ConnectionCredentialResolver.CertificateCredential;
import org.apache.dubbo.xds.security.authz.resolver.ConnectionCredentialResolver.ConnectionCredential;
import org.apache.dubbo.xds.security.authz.resolver.ConnectionCredentialResolver.SANType;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@Activate
public class SpiffeCredentialResolver implements CredentialResolver {

    private static final String SPIFFE_KEY = "spiffe";

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(SpiffeCredentialResolver.class);

    private static final String NAMESPACE = "ns";

    private static final String SERVICE_ACCOUNT = "sa";

    @Override
    public void appendRequestCredential(URL url, Invocation invocation, RequestCredential requestCredential) {
        Object credential = requestCredential.get(RequestAuthProperty.CONNECTION_CREDENTIAL);
        if (credential != null) {
            if (credential instanceof ConnectionCredential) {
                java.net.URI spiffe = readSpiffeId(((ConnectionCredential) credential).getCertificateCredentials());
                if (spiffe != null) {
                    requestCredential.add(RequestAuthProperty.TRUST_DOMAIN, spiffe.getHost());
                    requestCredential.add(RequestAuthProperty.KUBE_SOURCE_CLUSTER, spiffe.getHost());
                    requestCredential.add(RequestAuthProperty.WORKLOAD_ID, spiffe.getPath());

                    String hostWithPath = spiffe.getHost() + spiffe.getPath();
                    String[] segments = hostWithPath.split("/");
                    //  cluster.local[0]/ns[1]/default[2]/sa[3]/my-service-account[4] , len=5
                    if (segments.length == 5 && NAMESPACE.equals(segments[1]) && SERVICE_ACCOUNT.equals(segments[3])) {
                        String namespace = segments[2];
                        String serviceAccount = segments[4];
                        requestCredential.add(RequestAuthProperty.KUBE_SOURCE_NAMESPACE, namespace);
                        requestCredential.add(RequestAuthProperty.KUBE_SERVICE_PRINCIPAL, serviceAccount);
                        requestCredential.add(RequestAuthProperty.PRINCIPAL, hostWithPath);
                    } else {
                        logger.error("99-1", "", "", "Invalid SPIFFE ID format:" + spiffe);
                    }

                    requestCredential.add(RequestAuthProperty.SPIFFE_ID, spiffe.toString());
                }
            } else {
                logger.error(
                        "99-1",
                        "",
                        "",
                        "Got value with key=CONNECTION_CREDENTIAL but not a valid RequestCredential instance:"
                                + credential);
            }
        }
    }

    public java.net.URI readSpiffeId(List<CertificateCredential> credentials) {
        for (CertificateCredential credential : credentials) {
            Map<SANType, List<Object>> subjectAltNames = credential.getSubjectAltNames();
            if (subjectAltNames != null) {
                List<Object> list = subjectAltNames.get(SANType.URI);
                if (list != null && !list.isEmpty()) {
                    for (Object o : list) {
                        if (o instanceof String) {
                            try {
                                java.net.URI uri = new java.net.URI((String) o);
                                if (SPIFFE_KEY.equals(uri.getScheme())) {
                                    return uri;
                                }
                            } catch (URISyntaxException e) {
                                logger.warn(
                                        "99-1",
                                        "",
                                        "",
                                        "One SAN URI was ignored because it's not in valid URI format:" + o);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
