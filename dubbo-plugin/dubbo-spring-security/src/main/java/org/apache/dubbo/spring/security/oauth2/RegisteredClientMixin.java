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
package org.apache.dubbo.spring.security.oauth2;

import java.time.Instant;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class RegisteredClientMixin {

    /**
     * declare clientSettings and tokenSettings as Object type to avoid COMPILATION ERROR when compile it with jdk8
     * or jdk11, both ClientSettings and TokenSettings class file version are 61.0 which is higher than the version
     * which jdk8 (class file version: 52.0) or jdk11 (class file version: 55.0) could support.
     */
    @JsonCreator
    public RegisteredClientMixin(
            @JsonProperty("id") String id,
            @JsonProperty("clientId") String clientId,
            @JsonProperty("clientIdIssuedAt") Instant clientIdIssuedAt,
            @JsonProperty("clientSecret") String clientSecret,
            @JsonProperty("clientSecretExpiresAt") Instant clientSecretExpiresAt,
            @JsonProperty("clientName") String clientName,
            @JsonProperty("clientAuthenticationMethods") Set<ClientAuthenticationMethod> clientAuthenticationMethods,
            @JsonProperty("authorizationGrantTypes") Set<AuthorizationGrantType> authorizationGrantTypes,
            @JsonProperty("redirectUris") Set<String> redirectUris,
            @JsonProperty("postLogoutRedirectUris") Set<String> postLogoutRedirectUris,
            @JsonProperty("scopes") Set<String> scopes,
            @JsonProperty("clientSettings") Object clientSettings,
            @JsonProperty("tokenSettings") Object tokenSettings) {}
}
