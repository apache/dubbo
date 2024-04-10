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
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.kubernetes.KubeEnv;
import org.apache.dubbo.xds.security.api.AuthorizationException;
import org.apache.dubbo.xds.security.authz.RequestCredential;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

@Activate
public class TripleMeshRequestCredentialFactory implements CredentialFactory {

    private final KubeEnv kubeEnv;

    public TripleMeshRequestCredentialFactory(ApplicationModel applicationModel) {
        this.kubeEnv = applicationModel.getBeanFactory().getBean(KubeEnv.class);
    }

    @Override
    public RequestCredential getRequestCredential(URL url, Invocation invocation) {
        String token = (String) RpcContext.getServerAttachment().getObjectAttachment("authz");
        if (StringUtils.isEmpty(token)) {
            throw new AuthorizationException("Authorization token cannot be null or empty");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring("Bearer ".length());
        }

        DecodedJWT jwt = JWT.decode(token);

        long now = System.currentTimeMillis();
        String expAt = String.valueOf(jwt.getClaims().get("exp"));

        // convert millisecond -> second
        if (Long.parseLong(expAt) * 1000 < now) {
            throw new AuthorizationException("JWT token already expire, now:" + now + " exp:" + expAt);
        }

        String targetPath = invocation.getServiceName() + "/" + invocation.getMethodName();
        String httpMethod = "POST";
        Map<String, Object> kubeProps = jwt.getClaims().get("kubernetes.io").asMap();
        String issuer = jwt.getIssuer();
        String sub = jwt.getSubject();
        String namespace = null;

        String podName = null;
        String podId = null;
        Map<String, String> serviceAccount = Collections.emptyMap();
        if (kubeProps != null) {
            namespace = (String) kubeProps.get("namespace");
            serviceAccount = (Map<String, String>) kubeProps.get("serviceaccount");

            Map<String, String> pod = (Map<String, String>) kubeProps.get("pod");
            if (pod != null) {
                podName = pod.get("name");
                podId = pod.get("uid");
            }
        }
        String sourceService = serviceAccount.get("name");
        String uid = serviceAccount.get("uid");

        HttpRequestCredential requestCredential = new HttpRequestCredential(jwt.getClaims());

        String cluster = kubeEnv.getCluster();

        requestCredential.addByType(
                RequestAuthProperty.SERVICE_PRINCIPAL, cluster + "/ns/" + namespace + "/sa/" + sourceService);
        requestCredential.addByType(RequestAuthProperty.JWT_PRINCIPALS, issuer + "/" + sub);
        requestCredential.addByType(RequestAuthProperty.URL_PATH, targetPath);
        requestCredential.addByType(RequestAuthProperty.HTTP_METHOD, httpMethod);
        requestCredential.addByType(RequestAuthProperty.SOURCE_NAMESPACE, namespace);
        requestCredential.addByType(RequestAuthProperty.SOURCE_SERVICE_NAME, sourceService);
        requestCredential.addByType(RequestAuthProperty.SOURCE_POD_NAME, podName);
        requestCredential.addByType(RequestAuthProperty.SOURCE_POD_ID, podId);
        requestCredential.addByType(RequestAuthProperty.SOURCE_SERVICE_UID, uid);
        requestCredential.addByType(
                RequestAuthProperty.TARGET_VERSION,
                invocation.getInvoker().getUrl().getVersion());

        // 匹配时 下发的 acceptAud和这个audience列表有交集即可
        requestCredential.addByType(RequestAuthProperty.JWT_AUDIENCES, jwt.getAudience());

        Map<String, String> requestHttpHeaders = null; // TODO get full http headers
        requestCredential.addByType(RequestAuthProperty.JWT_FROM_HEADERS, requestHttpHeaders);

        //        requestCredential.addByType(RequestAuthProperty.JWT_NAME,);

        /*
         * User customized HTTP from params that submitted by GET or POST method.
         * TODO get params from request
         */
        Map<String, List<String>> httpRequestParams = null;
        requestCredential.addByType(RequestAuthProperty.JWT_FROM_PARAMS, httpRequestParams);

        requestCredential.addByType(RequestAuthProperty.JWT_ISSUER, issuer);
        requestCredential.addByType(RequestAuthProperty.JWKS, jwt);

        // TODO read other props
        // TODO  requestCredential.addByType(RequestAuthProperty.REQUESTED_SERVER_NAME,name);
        return requestCredential;
    }
}
