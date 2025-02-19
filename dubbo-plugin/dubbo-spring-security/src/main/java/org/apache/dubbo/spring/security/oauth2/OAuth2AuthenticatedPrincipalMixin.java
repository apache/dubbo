package org.apache.dubbo.spring.security.oauth2;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.security.core.GrantedAuthority;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class OAuth2AuthenticatedPrincipalMixin {

    @JsonCreator
    public OAuth2AuthenticatedPrincipalMixin(@JsonProperty("name") String name,
                                             @JsonProperty("attributes") Map<String, Object> attributes,
                                             @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities) {
    }
}
