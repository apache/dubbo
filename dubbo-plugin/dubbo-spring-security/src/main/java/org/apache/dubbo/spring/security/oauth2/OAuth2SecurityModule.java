package org.apache.dubbo.spring.security.oauth2;

import java.util.ArrayList;
import java.util.Collections;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

public class OAuth2SecurityModule extends SimpleModule {

    public OAuth2SecurityModule() {
        super(OAuth2SecurityModule.class.getName());
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(OAuth2AuthenticatedPrincipal.class, OAuth2AuthenticatedPrincipalMixin.class);
        context.setMixInAnnotations(DefaultOAuth2AuthenticatedPrincipal.class, OAuth2AuthenticatedPrincipalMixin.class);
        context.setMixInAnnotations(BearerTokenAuthentication.class, BearerTokenAuthenticationMixin.class);
        context.setMixInAnnotations(OAuth2ClientAuthenticationToken.class, OAuth2ClientAuthenticationTokenMixin.class);
        context.setMixInAnnotations(ClientAuthenticationMethod.class, ClientAuthenticationMethodMixin.class);
        context.setMixInAnnotations(RegisteredClient.class, RegisteredClientMixin.class);
        context.setMixInAnnotations(AuthorizationGrantType.class, AuthorizationGrantTypeMixin.class);
        context.setMixInAnnotations(ClientSettings.class, ClientSettingsMixin.class);
        context.setMixInAnnotations(TokenSettings.class, TokenSettingsMixin.class);
        context.setMixInAnnotations(Collections.unmodifiableCollection(new ArrayList<>())
                .getClass(), UnmodifiableCollectionMixin.class);
    }
}
