package com.alibaba.dubbo.oauth2.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.oauth2.property.OAuth2Properties;
import com.alibaba.dubbo.oauth2.property.TokenDetails;
import com.alibaba.dubbo.oauth2.property.UserDetails;
import com.alibaba.dubbo.oauth2.support.RestInstance;
import com.alibaba.dubbo.oauth2.support.util.OAuth2PropertyUtil;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.springframework.http.HttpMethod.GET;

/**
 * Created by wuyu on 2017/1/7.
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER})
public class OAuth2Filter implements Filter {


    private static final Logger logger = LoggerFactory.getLogger(OAuth2Filter.class);

    private RestTemplate restTemplate = RestInstance.restTemplate(3000, 20);

    private Map<String, UserDetails> cache = new LinkedHashMap<String, UserDetails>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, UserDetails> eldest) {
            return size() > 100;
        }
    };

    private OAuth2Properties oAuth2Properties;

    private volatile TokenDetails tokenDetails;


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        boolean consumerSide = RpcContext.getContext().isConsumerSide();
        boolean providerSide = RpcContext.getContext().isProviderSide();


        if (oAuth2Properties == null) {
            this.oAuth2Properties = OAuth2PropertyUtil.loadOAuth2Properties();
            if (oAuth2Properties == null) {
                throw new RpcException("load oauth property error!");
            }
        }

        if (consumerSide) {
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

        if (providerSide) {
            //拿取token作为权限字段 ROLE_ADMIN,ROLE_USER
            String roles = invoker.getUrl().getParameter("token");
            if (roles == null) {
                return invoker.invoke(invocation);
            }

            String access_token = invocation.getAttachment("access_token");
            UserDetails userInfo = getUserInfo(access_token);
            Set<String> authorities = userInfo.getAuthorities();

            for (String role : roles.split(",")) {
                if (authorities.contains(role.trim())) {
                    return invoker.invoke(invocation);
                }
            }
            throw new RpcException("Permission denied!:" + JSON.toJSONString(userInfo));
        }

        return invoker.invoke(invocation);
    }


    private UserDetails getUserInfo(String token) {

        UserDetails userDetails = cache.get(token);
        if (userDetails != null) {
            return userDetails;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        ResponseEntity<JSONObject> responseEntity = restTemplate.exchange(oAuth2Properties.getUserInfoUrl(),
                GET,
                new HttpEntity(headers),
                JSONObject.class);
        JSONObject userInfo = responseEntity.getBody();
        if (responseEntity.getStatusCodeValue() != 200) {
            throw new RpcException(userInfo.toJSONString());
        }

        JSONArray authorities = userInfo.getJSONArray("authorities");
        String principal = userInfo.getString("principal");
        userDetails = new UserDetails();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < authorities.size(); i++) {
            JSONObject authorityJson = authorities.getJSONObject(i);
            String authority = authorityJson.getString("authority");
            set.add(authority);
        }
        userDetails.setAuthorities(set);
        userDetails.setPrincipal(principal);
        cache.put(token, userDetails);
        return userDetails;
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


    private TokenDetails getTokenDetails() {
        HttpEntity httpEntity = oAuth2Entity();
        return restTemplate.postForObject(oAuth2Properties.getAccessTokenUri(), httpEntity, TokenDetails.class);
    }


    public TokenDetails refresh() {
        HttpEntity httpEntity = oAuth2Entity();
        return restTemplate.postForObject(oAuth2Properties.getAccessTokenUri(), httpEntity, TokenDetails.class);
    }


}
