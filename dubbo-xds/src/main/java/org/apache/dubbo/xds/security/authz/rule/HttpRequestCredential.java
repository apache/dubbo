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

import org.apache.dubbo.xds.security.authz.RequestCredential;

import java.util.HashMap;
import java.util.Map;

import com.auth0.jwt.interfaces.Claim;

public class HttpRequestCredential implements RequestCredential {

    private final Map<String, Claim> jwtClaims;

    /**
     * path-> credential properties
     */
    private final Map<RequestAuthProperty, Object> authProperties;

    public HttpRequestCredential(Map<String, Claim> jwtClaims) {
        this.jwtClaims = jwtClaims;
        this.authProperties = new HashMap<>();
    }

    @Override
    public Object getRequestProperty(RequestAuthProperty propertyType) {
        return authProperties.get(propertyType);
    }


    public void addByType(RequestAuthProperty propertyType, Object value) {
        this.authProperties.put(propertyType,value);
    }

}
