package com.alibaba.dubbo.oauth2.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.oauth2.property.TokenDetails;
import com.alibaba.dubbo.oauth2.property.UserDetails;
import com.alibaba.dubbo.oauth2.support.OAuth2Service;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.fastjson.JSON;

import java.util.Set;

/**
 * Created by wuyu on 2017/1/7.
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER}, value = "oAuth2Filter")
public class OAuth2Filter implements Filter {


    private static final Logger logger = LoggerFactory.getLogger(OAuth2Filter.class);

    private OAuth2Service oAuth2Service = OAuth2Service.getInstance();


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        boolean consumerSide = RpcContext.getContext().isConsumerSide();
        boolean providerSide = RpcContext.getContext().isProviderSide();


        if (consumerSide) {
            TokenDetails tokenDetails = oAuth2Service.getTokenDetails();
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
            if (access_token == null) {
                throw new RpcException("must carry access_token!");
            }
            UserDetails userInfo = oAuth2Service.getUserInfo(access_token);
            //传递身份信息
            invocation.getAttachments().put("principal", JSON.toJSONString(userInfo));
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


}
