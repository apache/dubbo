package com.alibaba.dubbo.oauth2.support.util;

import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.oauth2.property.OAuth2Properties;
import com.alibaba.dubbo.rpc.RpcException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * Created by wuyu on 2017/1/7.
 */
public class OAuth2PropertyUtil {


    public static synchronized OAuth2Properties loadOAuth2Properties() {
        OAuth2Properties oAuth2Properties = null;
        Set<ApplicationContext> springContext = SpringUtil.getApplicationContexts();
        for (ApplicationContext applicationContext : springContext) {
            String[] beanNamesForType = applicationContext.getBeanNamesForType(OAuth2Properties.class);
            if (beanNamesForType.length > 0) {
                oAuth2Properties = applicationContext.getBean(OAuth2Properties.class);
            }
        }

        if (oAuth2Properties == null) {
            oAuth2Properties = new OAuth2Properties();
            ClassPathResource classPathResource = new ClassPathResource("META-INF/dubbo/oauth2/oauth2.properties");
            InputStream inputStream = null;
            try {
                inputStream = classPathResource.getInputStream();
                Properties properties = new Properties();
                properties.load(inputStream);
                String clientId = properties.getProperty("clientId");
                String clientSecret = properties.getProperty("clientSecret");
                String grantType = properties.getProperty("grantType");
                String scope = properties.getProperty("scope");
                String accessTokenUri = properties.getProperty("accessTokenUri");
                String userInfoUri = properties.getProperty("userInfoUri");
                String refreshUri = properties.getProperty("refreshUri");
                String userAuthorizationUri = properties.getProperty("userAuthorizationUri");
                String redirectUri = properties.getProperty("redirectUri");
                oAuth2Properties.setClientId(clientId);
                oAuth2Properties.setClientSecret(clientSecret);
                oAuth2Properties.setGrantType(grantType);
                oAuth2Properties.setScope(scope);
                oAuth2Properties.setAccessTokenUri(accessTokenUri);
                oAuth2Properties.setUserAuthorizationUri(userAuthorizationUri);
                oAuth2Properties.setUserInfoUrl(userInfoUri);
                oAuth2Properties.setRedirectUri(redirectUri);
                oAuth2Properties.setRefreshUrl(refreshUri);
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

        return oAuth2Properties;
    }
}
