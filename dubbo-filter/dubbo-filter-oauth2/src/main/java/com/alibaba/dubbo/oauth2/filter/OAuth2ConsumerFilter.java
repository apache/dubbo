package com.alibaba.dubbo.oauth2.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.oauth2.property.OAuth2Properties;
import com.alibaba.dubbo.oauth2.property.TokenDetails;
import com.alibaba.dubbo.oauth2.support.RestInstance;
import com.alibaba.dubbo.oauth2.support.util.OAuth2PropertyUtil;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Date;

/**
 * Created by wuyu on 2017/1/7.
 */
@Activate(group = Constants.CONSUMER)
public class OAuth2ConsumerFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ConsumerFilter.class);

    private RestTemplate restTemplate = RestInstance.restTemplate(10000, 20);

    private volatile TokenDetails tokenDetails;

    private OAuth2Properties oAuth2Properties;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        if (oAuth2Properties == null) {
            this.oAuth2Properties = OAuth2PropertyUtil.loadOAuth2Properties();
            if (oAuth2Properties == null) {
                throw new RpcException("load oauth property error!");
            }
        }


        if (tokenDetails == null) {
            tokenDetails = getTokenDetails();
            logger.info("get token " + tokenDetails.toString());
        } else {
            long expire = tokenDetails.getCreatedTime() + tokenDetails.getExpiresIn();
            long date = new Date().getTime() / 1000;
            if (expire < date) {
                tokenDetails = getTokenDetails();
            }
        }

        invocation.getAttachments().put("access_token", tokenDetails.getAccessToken());
        return invoker.invoke(invocation);
    }


    private TokenDetails getTokenDetails() {
        HttpEntity httpEntity = oAuth2Entity();
        return restTemplate.postForObject(oAuth2Properties.getAccessTokenUri(), httpEntity, TokenDetails.class);
    }


    public TokenDetails refresh() {
        HttpEntity httpEntity = oAuth2Entity();
        return restTemplate.postForObject(oAuth2Properties.getAccessTokenUri(), httpEntity, TokenDetails.class);
    }

    public HttpEntity oAuth2Entity() {
        String clientId = oAuth2Properties.getClientId();
        String clientSecret = oAuth2Properties.getClientSecret();
        //OAuth2 Form 表单
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("grant_type", oAuth2Properties.getGrantType());
        parameters.add("scope", oAuth2Properties.getScope());
        parameters.add("client_secret", oAuth2Properties.getClientSecret());
        parameters.add("client_id", oAuth2Properties.getClientId());
        parameters.add("redirect_uri", oAuth2Properties.getRedirectUri());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        //basic验证,base64加密,客户端id,安全key
        headers.add("Authorization", "Basic " + Base64Utils.encodeToString((clientId + ":" + clientSecret).getBytes()));
        return new HttpEntity(parameters, headers);
    }


}
