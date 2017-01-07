package com.alibaba.dubbo.oauth2.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.oauth2.property.OAuth2Properties;
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
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpMethod.GET;

/**
 * Created by wuyu on 2017/1/7.
 */
@Activate(group = Constants.PROVIDER)
public class OAuth2ProviderFilter implements Filter {


    private static final Logger logger = LoggerFactory.getLogger(OAuth2ConsumerFilter.class);

    private RestTemplate restTemplate = RestInstance.restTemplate(3000, 20);

    private Map<String, UserDetails> cache = new LinkedHashMap<String, UserDetails>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, UserDetails> eldest) {
            return size() > 100;
        }
    };

    private OAuth2Properties oAuth2Properties;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        if (oAuth2Properties == null) {
            oAuth2Properties = OAuth2PropertyUtil.loadOAuth2Properties();
        }

        //拿取token作为权限字段 ROLE_ADMIN,ROLE_USER
        String roles = invoker.getUrl().getParameter("token");
        if (roles == null) {
            return invoker.invoke(invocation);
        }

        String access_token = invocation.getAttachment("access_token");
        UserDetails userInfo = getUserInfo(access_token);
        Set<String> authorities = userInfo.getAuthorities();

        for (String role : roles.split(",")) {
            if (!authorities.contains(role.trim())) {
                return invoker.invoke(invocation);

            }
        }

        throw new RpcException("Permission denied!:" + JSON.toJSONString(userInfo));

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


}
