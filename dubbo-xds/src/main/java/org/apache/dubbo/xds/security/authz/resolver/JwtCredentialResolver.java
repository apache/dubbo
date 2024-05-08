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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.xds.security.authz.RequestCredential;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

@Activate(order = -1000)
public class JwtCredentialResolver implements CredentialResolver {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(JwtCredentialResolver.class);

    @Override
    public void appendRequestCredential(URL url, Invocation invocation, RequestCredential requestCredential) {
        String token = (String) RpcContext.getServerAttachment().getObjectAttachment("authz");
        if (StringUtils.isEmpty(token)) {
            return;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring("Bearer ".length());
        }

        DecodedJWT jwt = JWT.decode(token);
        long now = System.currentTimeMillis();
        String expAt = String.valueOf(jwt.getClaims().get("exp"));

        // convert millisecond -> second
        if (Long.parseLong(expAt) * 1000 < now) {
            logger.warn("99-0", "", "", "Request JWT token already expire, now:" + now + " exp:" + expAt);
        }

        String issuer = jwt.getIssuer();
        String sub = jwt.getSubject();
        requestCredential.add(RequestAuthProperty.JWT_PRINCIPALS, issuer + "/" + sub);
        requestCredential.add(RequestAuthProperty.JWT_AUDIENCES, jwt.getAudience());
        requestCredential.add(RequestAuthProperty.JWT_ISSUER, issuer);
        requestCredential.add(RequestAuthProperty.DECODED_JWT, jwt);
        // use jwks to validate this jwt
        requestCredential.add(RequestAuthProperty.JWKS, jwt);
    }
}
