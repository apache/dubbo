package com.alibaba.dubbo.oauth2.support;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.oauth2.property.OAuth2Properties;
import com.alibaba.dubbo.oauth2.property.TokenDetails;
import com.alibaba.dubbo.oauth2.property.UserDetails;
import com.alibaba.dubbo.oauth2.support.util.OAuth2PropertyUtil;
import com.alibaba.dubbo.rpc.RpcException;
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
 * Created by wuyu on 2017/1/8.
 */
public class OAuth2Service {

    private OAuth2Properties oAuth2Properties;

    private volatile TokenDetails tokenDetails;

    private Map<String, UserDetails> cache = new LinkedHashMap<String, UserDetails>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, UserDetails> eldest) {
            return size() > 100;
        }
    };

    private RestTemplate restTemplate = RestInstance.restTemplate(3000, 20);
    private static final Logger logger = LoggerFactory.getLogger(OAuth2Service.class);

    private static OAuth2Service oAuth2Service = new OAuth2Service();

    public OAuth2Service() {
        if (oAuth2Properties == null) {
            this.oAuth2Properties = OAuth2PropertyUtil.loadOAuth2Properties();
            if (oAuth2Properties == null) {
                throw new RpcException("load oauth property error!");
            }
        }
    }

    public UserDetails getUserInfo(String token) {

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


    public TokenDetails getTokenDetails() {
        if (tokenDetails == null) {
            HttpEntity httpEntity = oAuth2Entity();
            tokenDetails = restTemplate.postForObject(oAuth2Properties.getAccessTokenUri(), httpEntity, TokenDetails.class);
            logger.info("get token " + tokenDetails.toString());
        } else {
            long expire = tokenDetails.getCreatedTime() + tokenDetails.getExpiresIn();
            long date = new Date().getTime() / 1000;
            if (expire < date) {
                HttpEntity httpEntity = oAuth2Entity();
                tokenDetails = restTemplate.postForObject(oAuth2Properties.getAccessTokenUri(), httpEntity, TokenDetails.class);
            }
        }
        return tokenDetails;
    }


    public TokenDetails refresh() {
        HttpEntity httpEntity = oAuth2Entity();
        return restTemplate.postForObject(oAuth2Properties.getAccessTokenUri(), httpEntity, TokenDetails.class);
    }

    public static OAuth2Service getInstance() {
        return oAuth2Service;
    }

}
