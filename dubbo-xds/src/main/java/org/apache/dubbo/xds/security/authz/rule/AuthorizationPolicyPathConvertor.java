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

public class AuthorizationPolicyPathConvertor {

    public static RequestAuthProperty convert(String path) {

        switch (path) {
            case "rules.to.operation.paths":
                return RequestAuthProperty.URL_PATH;
            case "rules.to.operation.methods":
                return RequestAuthProperty.METHODS;
            case "rules.from.source.namespaces":
                return RequestAuthProperty.SOURCE_NAMESPACE;
            case "rules.source.service.name":
                return RequestAuthProperty.SOURCE_SERVICE_NAME;
            case "rules.source.service.uid":
                return RequestAuthProperty.SOURCE_SERVICE_UID;
            case "rules.source.pod.name":
                return RequestAuthProperty.SOURCE_POD_NAME;
            case "rules.source.pod.id":
                return RequestAuthProperty.SOURCE_POD_ID;
            case "rules.from.source.principals":
                return RequestAuthProperty.SERVICE_PRINCIPAL;
            case "rules.to.operation.version":
                return RequestAuthProperty.TARGET_VERSION;
            default:
                throw new RuntimeException("not supported path:" + path);
        }
    }
}
