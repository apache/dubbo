package com.alibaba.dubbo.rpc.protocol.springmvc.oauth2;

import com.alibaba.dubbo.oauth2.property.TokenDetails;
import com.alibaba.dubbo.oauth2.support.OAuth2Service;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Created by wuyu on 2017/1/8.
 */
public class FeignOAuth2Interceptor implements RequestInterceptor {

    private OAuth2Service oAuth2Service = OAuth2Service.getInstance();

    @Override
    public void apply(RequestTemplate template) {
        TokenDetails tokenDetails = oAuth2Service.getTokenDetails();
        template.header("Authorization", "Bearer " + tokenDetails.getAccessToken());
    }
}
