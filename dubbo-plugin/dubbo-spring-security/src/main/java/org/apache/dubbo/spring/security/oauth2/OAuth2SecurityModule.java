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

import org.apache.dubbo.common.utils.ClassUtils;

import java.util.ArrayList;
import java.util.Collections;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class OAuth2SecurityModule extends SimpleModule {

    public OAuth2SecurityModule() {
        super(OAuth2SecurityModule.class.getName());
    }

    @Override
    public void setupModule(SetupContext context) {
        setMixInAnnotations(
                context,
                "org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal",
                "org.apache.dubbo.spring.security.oauth2.OAuth2AuthenticatedPrincipalMixin");
        setMixInAnnotations(
                context,
                "org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal",
                "org.apache.dubbo.spring.security.oauth2.OAuth2AuthenticatedPrincipalMixin");
        setMixInAnnotations(
                context,
                "org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication",
                "org.apache.dubbo.spring.security.oauth2.BearerTokenAuthenticationMixin");
        setMixInAnnotations(
                context,
                "org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken",
                "org.apache.dubbo.spring.security.oauth2.OAuth2ClientAuthenticationTokenMixin");
        setMixInAnnotations(
                context,
                "org.springframework.security.oauth2.core.ClientAuthenticationMethod",
                ClientAuthenticationMethodMixin.class);
        setMixInAnnotations(
                context,
                "org.springframework.security.oauth2.server.authorization.client.RegisteredClient",
                "org.apache.dubbo.spring.security.oauth2.RegisteredClientMixin");
        setMixInAnnotations(
                context,
                "org.springframework.security.oauth2.core.AuthorizationGrantType",
                AuthorizationGrantTypeMixin.class);
        setMixInAnnotations(
                context,
                "org.springframework.security.oauth2.server.authorization.settings.ClientSettings",
                ClientSettingsMixin.class);
        setMixInAnnotations(
                context,
                "org.springframework.security.oauth2.server.authorization.settings.TokenSettings",
                TokenSettingsMixin.class);
        context.setMixInAnnotations(
                Collections.unmodifiableCollection(new ArrayList<>()).getClass(), UnmodifiableCollectionMixin.class);
    }

    private void setMixInAnnotations(SetupContext context, String oauth2ClassName, String mixinClassName) {
        Class<?> oauth2Class = loadClassIfPresent(oauth2ClassName);
        if (oauth2Class != null) {
            context.setMixInAnnotations(oauth2Class, loadClassIfPresent(mixinClassName));
        }
    }

    private void setMixInAnnotations(SetupContext context, String oauth2ClassName, Class<?> mixinClass) {
        Class<?> oauth2Class = loadClassIfPresent(oauth2ClassName);
        if (oauth2Class != null) {
            context.setMixInAnnotations(oauth2Class, mixinClass);
        }
    }

    private Class<?> loadClassIfPresent(String oauth2ClassName) {
        try {
            return ClassUtils.forName(oauth2ClassName, OAuth2SecurityModule.class.getClassLoader());

        } catch (Throwable ignored) {
        }
        return null;
    }
}
