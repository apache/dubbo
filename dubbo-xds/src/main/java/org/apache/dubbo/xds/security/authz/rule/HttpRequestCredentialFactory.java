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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.xds.security.api.AuthorizationException;
import org.apache.dubbo.xds.security.authz.RequestCredential;

import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

@Activate
public class HttpRequestCredentialFactory implements CredentialFactory {

    @Override
    public RequestCredential getRequestCredential(URL url, Invocation invocation) {
        String authorizationToken = RpcContext.getServerAttachment().getAttachment("Authorization");
        if (StringUtils.isEmpty(authorizationToken)) {
            throw new AuthorizationException("Authorization token cannot be null or empty");
        }
        if (!authorizationToken.startsWith("Bearer ")) {
            throw new AuthorizationException("Invalid bearer authorization token:" + authorizationToken);
        }

        authorizationToken = authorizationToken.substring("Bearer ".length());
        DecodedJWT jwt = JWT.decode(authorizationToken);

        Map<String, String> payload = JSON.parseObject(jwt.getPayload(), Map.class, String.class, String.class);

        //        TODO: 校验签名，RSA256
        //        String signature = jwt.getSignature();
        //        String secret = "secret";
        //        Algorithm algorithm = Algorithm.HMAC256(secret);

        long now = System.currentTimeMillis();

        String expAt = payload.get("exp");
        if (Long.parseLong(expAt) < now) {
            throw new AuthorizationException("Jwt token already expire, now:" + now + " exp:" + expAt);
        }
        String targetPath = invocation.getInvoker().getUrl().getPath();

        String httpMethod = null; // TODO

        return new HttpBasedRequestCredential(payload.get("iss"), payload.get("sub"), targetPath, httpMethod, payload);
    }
}
