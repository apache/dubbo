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

package org.apache.dubbo.spring.security.jackson;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Duration;
import java.time.Instant;

public class ObjectMapperCodecTest {

    private ObjectMapperCodec mapper = new ObjectMapperCodec();

    @Test
    public void testOAuth2AuthorizedClientCodec() {
        ClientRegistration clientRegistration = clientRegistration().build();
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(clientRegistration, "principal-name", noScopes());

        String content = mapper.serialize(authorizedClient);

        OAuth2AuthorizedClient deserialize = mapper.deserialize(content.getBytes(),
            OAuth2AuthorizedClient.class);

        Assertions.assertNotNull(deserialize);
    }


    public static ClientRegistration.Builder clientRegistration() {
        // @formatter:off
        return ClientRegistration.withRegistrationId("registration-id")
            .redirectUri("http://localhost/uua/oauth2/code/{registrationId}")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .scope("read:user")
            .authorizationUri("https://example.com/login/oauth/authorize")
            .tokenUri("https://example.com/login/oauth/access_token")
            .jwkSetUri("https://example.com/oauth2/jwk")
            .issuerUri("https://example.com")
            .userInfoUri("https://api.example.com/user")
            .userNameAttributeName("id")
            .clientName("Client Name")
            .clientId("client-id")
            .clientSecret("client-secret");
        // @formatter:on
    }

    public static OAuth2AccessToken noScopes() {
        return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "no-scopes", Instant.now(),
            Instant.now().plus(Duration.ofDays(1)));
    }
}
