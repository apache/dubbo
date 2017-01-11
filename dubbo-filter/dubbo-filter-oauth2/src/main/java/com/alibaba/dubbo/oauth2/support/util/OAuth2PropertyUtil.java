package com.alibaba.dubbo.oauth2.support.util;

import com.alibaba.dubbo.oauth2.property.OAuth2Properties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * Created by wuyu on 2017/1/7.
 */
public class OAuth2PropertyUtil {


    public static synchronized OAuth2Properties loadOAuth2Properties() {
        OAuth2Properties oAuth2Properties = new OAuth2Properties();
        String clientId = null;
        String clientSecret = null;
        String grantType = "client_details";
        String scope = null;
        String accessTokenUri = null;
        String userInfoUri = null;
        String refreshUri = null;
        String userAuthorizationUri = null;
        String redirectUri = null;

        //判断是否存在 spring oauth2 配置
        boolean resourceIsPresent = ClassUtils.isPresent("org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails", OAuth2PropertyUtil.class.getClassLoader());
        if (resourceIsPresent) {
            Environment environment = SpringUtil.getBean(Environment.class);
            userInfoUri = environment.getProperty("security.oauth2.resource.user-info-uri");
            accessTokenUri = environment.getProperty("security.oauth2.client.access-token-uri");
            clientId = environment.getProperty("security.oauth2.client.client-id");
            clientSecret = environment.getProperty("security.oauth2.client.client-secret");
            scope = environment.getProperty("security.oauth2.client.scope");
        }
        if (oAuth2Properties.getClientId() == null) {
            Set<ApplicationContext> springContext = SpringUtil.getApplicationContexts();
            for (ApplicationContext applicationContext : springContext) {
                String[] beanNamesForType = applicationContext.getBeanNamesForType(OAuth2Properties.class);
                if (beanNamesForType.length > 0) {
                    oAuth2Properties = applicationContext.getBean(OAuth2Properties.class);
                }
            }
        }
        if (oAuth2Properties.getClientId() == null) {
            oAuth2Properties = new OAuth2Properties();
            ClassPathResource classPathResource = new ClassPathResource("META-INF/dubbo/oauth2/oauth2.properties");
            InputStream inputStream = null;
            try {
                inputStream = classPathResource.getInputStream();
                Properties properties = new Properties();
                properties.load(inputStream);
                clientId = properties.getProperty("clientId");
                clientSecret = properties.getProperty("clientSecret");
                grantType = properties.getProperty("grantType");
                scope = properties.getProperty("scope");
                accessTokenUri = properties.getProperty("accessTokenUri");
                userInfoUri = properties.getProperty("userInfoUri");
                refreshUri = properties.getProperty("refreshUri");
                userAuthorizationUri = properties.getProperty("userAuthorizationUri");
                redirectUri = properties.getProperty("redirectUri");

            } catch (IOException e) {
                return null;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {

                    }
                }
            }
        }

        oAuth2Properties.setClientId(clientId);
        oAuth2Properties.setClientSecret(clientSecret);
        oAuth2Properties.setGrantType(grantType);
        oAuth2Properties.setScope(scope);
        oAuth2Properties.setAccessTokenUri(accessTokenUri);
        oAuth2Properties.setUserAuthorizationUri(userAuthorizationUri);
        oAuth2Properties.setUserInfoUrl(userInfoUri);
        oAuth2Properties.setRedirectUri(redirectUri);
        oAuth2Properties.setRefreshUrl(refreshUri);

        return oAuth2Properties;
    }
}
