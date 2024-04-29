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
package org.apache.dubbo.xds.security.authz.rule;

public enum RequestAuthProperty {

    /**
     * Request header
     * Rule attribution:principal
     * <p>
     * Rule modification section：
     * <p>
     * when:
     * 1)rules:when (request.headers[xxx])
     */
    HEADER,
    /**
     * Direct request ip address
     * Rule attribution:principal
     * <p>
     * Rule modification section：
     * <p>
     * from:
     * 1)rules:from:source:ipBlocks
     * 2)rules:from:source:notIpBlocks
     * <p>
     * when:
     * 1)rules:when (source.ip)
     */
    DIRECT_REMOTE_IP,

    /**
     * The original client IP address determined by the X-Forwarded-For request header or proxy protocol
     * Rule attribution:principal
     * <p>
     * Rule modification section：
     * <p>
     * from:
     * 1)rules:from:source:remoteIpBlocks
     * 2)rules:from:source:notRemoteIpBlocks
     * <p>
     * when:
     * 1)rules:when (remote.ip)
     */
    REMOTE_IP,

    /**
     * Identity in jwt = issuer + "/" + subject
     * Rule attribution:principal
     * <p>
     * Rule modification section：
     * <p>
     * from:
     * 1)rules:from:source:requestPrincipals
     * <p>
     * when:
     * 1)rules:when (request.auth.principal)
     */
    JWT_PRINCIPALS,

    /**
     * Audience in jwt
     * Rule attribution:principal
     * <p>
     * Rule modification section：
     * <p>
     * when:
     * 1)rules:when (request.auth.claims[xxx])
     */
    JWT_CLAIMS,

    /**
     * Azp in jwt: Authorized party - the party to which the ID Token was issued
     * rule attribution:principal
     * <p>
     * Rule modification section：
     * <p>
     * when:
     * 1)rules:when (request.auth.presenter)
     */
    JWT_PRESENTERS,
    /**
     * What should the requester's identity be
     * Rule attribution:principal
     * <p>
     * Rule modification section：
     * <p>
     * from:
     * 1)rules:from:source:principals
     * 2)rules:from:source:notPrincipals
     * 3)rules:from:namespaces
     * Concatenate regular expressions as formal principals,for example：namespaces: ["namespace1"]->  .*
     * /ns/namespace1/.*
     * 4)rules:from:notNamespaces
     * <p>
     * when:
     * 1)rules:when (source.principal)
     * 2)rules:when (source.namespace)
     */
    AUTHENTICATED,

    /**
     * Server ip
     * Rule attribution:permission
     * <p>
     * Rule modification section：
     * <p>
     * when:
     * 1)rules:when (destination.ip)
     */
    DESTINATION_IP,

    /**
     * Server hosts
     * <p>
     * Rule modification section：
     * <p>
     * to:
     * 1)rules:to:operation:hosts
     * 2)rules:to:operation:notHosts
     */
    HOSTS,

    /**
     * Server url path
     * Rule attribution:permission
     * <p>
     * Rule modification section：
     * <p>
     * to:
     * 1)rules:to:operation:paths
     * 2)rules:to:operation:notPaths
     */
    URL_PATH,

    /**
     * Server port
     * Rule attribution:permission
     * <p>
     * Rule modification section：
     * <p>
     * to:
     * 1)rules:to:operation:ports
     * 2)rules:to:operation:notPorts
     * <p>
     * when:
     * 1)rules:when (destination.port)
     */
    DESTINATION_PORT,

    /**
     * Server methods
     * Rule attribution:permission
     * <p>
     * Rule modification section：
     * <p>
     * to:
     * 1)rules:to:operation:methods
     * 2)rules:to:operation:notMethods
     */
    METHODS,

    /**
     * Server sni : request.getServerName()
     * Rule attribution:permission
     * <p>
     * Rule modification section：
     * <p>
     * when:
     * 1)rules:when (connection.sni)
     */
    REQUESTED_SERVER_NAME,

    SERVICE_PRINCIPAL,

    SOURCE_NAMESPACE,

    HTTP_METHOD,

    SOURCE_SERVICE_NAME,

    SOURCE_POD_NAME,

    SOURCE_POD_ID,

    SOURCE_SERVICE_UID,

    TARGET_VERSION,

    SOURCE_CLUSTER,

    SOURCE_METADATA,

    // JWT rules

    /**
     * Audience in jwt
     * Rule attribution:principal
     * <p>
     * Rule modification section：
     * <p>
     * when:
     * 1)rules:when (request.auth.audiences)
     */
    JWT_AUDIENCES,
    JWT_NAME,

    JWT_ISSUER,
    JWKS,
    JWT_FROM_PARAMS,
    JWT_FROM_HEADERS,

    /**
     * Type of opposite request agent
     */
    OPPOSITE_AGENT,
    /**
     * spiffe://{trust_domain}/{workload_identity}
     */
    TRUST_DOMAIN,
    WORKLOAD_ID;
}
