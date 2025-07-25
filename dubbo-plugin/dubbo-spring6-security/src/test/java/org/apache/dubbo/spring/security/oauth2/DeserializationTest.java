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

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.spring.security.jackson.ObjectMapperCodec;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

@SpringBootTest(
        properties = {"dubbo.registry.address=N/A"},
        classes = {DeserializationTest.class})
@Configuration
public class DeserializationTest {

    private static ObjectMapperCodec mapper;

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
        mapper = ApplicationModel.defaultModel().getDefaultModule().getBean(ObjectMapperCodec.class);
    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
    }

    @Test
    public void bearerTokenAuthenticationTest() {
        BearerTokenAuthentication bearerTokenAuthentication = new BearerTokenAuthentication(
                new DefaultOAuth2AuthenticatedPrincipal(
                        "principal-name",
                        Collections.singletonMap("name", "kali"),
                        Collections.singleton(new SimpleGrantedAuthority("1"))),
                new OAuth2AccessToken(TokenType.BEARER, "111", Instant.MIN, Instant.MAX),
                Collections.emptyList());
        String content = mapper.serialize(bearerTokenAuthentication);

        BearerTokenAuthentication deserialize = mapper.deserialize(content.getBytes(), BearerTokenAuthentication.class);

        Assertions.assertNotNull(deserialize);
    }

    @Test
    public void oauth2ClientAuthenticationTokenTest() {
        OAuth2ClientAuthenticationToken oAuth2ClientAuthenticationToken = new OAuth2ClientAuthenticationToken(
                "client-id", ClientAuthenticationMethod.CLIENT_SECRET_POST, "111", Collections.emptyMap());

        String content = mapper.serialize(oAuth2ClientAuthenticationToken);

        OAuth2ClientAuthenticationToken deserialize =
                mapper.deserialize(content.getBytes(), OAuth2ClientAuthenticationToken.class);

        Assertions.assertNotNull(deserialize);
    }

    @Test
    public void registeredClientTest() {
        RegisteredClient registeredClient = RegisteredClient.withId("id")
                .clientId("client-id")
                .clientName("client-name")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://example.com")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT)
                .clientSecret("client-secret")
                .clientIdIssuedAt(Instant.MIN)
                .clientSecretExpiresAt(Instant.MAX)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(Duration.ofSeconds(1000))
                        .build())
                .clientSettings(ClientSettings.builder()
                        .setting("name", "value")
                        .requireProofKey(true)
                        .build())
                .build();

        String content = mapper.serialize(registeredClient);

        RegisteredClient deserialize = mapper.deserialize(content.getBytes(), RegisteredClient.class);

        Assertions.assertEquals(registeredClient, deserialize);
    }

    @Configuration
    @ImportResource("classpath:/dubbo-test.xml")
    public static class OAuth2SecurityTestConfiguration {}
}
