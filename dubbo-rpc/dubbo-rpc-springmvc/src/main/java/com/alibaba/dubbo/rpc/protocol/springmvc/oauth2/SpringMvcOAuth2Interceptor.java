package com.alibaba.dubbo.rpc.protocol.springmvc.oauth2;

import com.alibaba.dubbo.oauth2.property.UserDetails;
import com.alibaba.dubbo.oauth2.support.OAuth2Service;
import org.springframework.aop.support.AopUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wuyu on 2017/1/8.
 */
public class SpringMvcOAuth2Interceptor implements HandlerInterceptor {

    private Map<String, String> roles = new HashMap<>();

    private OAuth2Service oAuth2Service = OAuth2Service.getInstance();

    private boolean enable = false;

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        if (!enable) {
            return true;
        }
        if (o instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) o;
            Class<?> targetClass = AopUtils.getTargetClass(handlerMethod.getBean());
            Class<?>[] interfaces = targetClass.getInterfaces();
            for (Class<?> iFace : interfaces) {
                if (roles.containsKey(iFace.getName())) {
                    String authorization = httpServletRequest.getHeader("Authorization");
                    if (authorization != null) {
                        String accessToken = authorization.replace("Bearer", "").trim();
                        UserDetails userDetails = oAuth2Service.getUserInfo(accessToken);
                        String role = roles.get(targetClass.getName());
                        if (userDetails.getAuthorities().contains(role)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }

    public Map<String, String> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, String> roles) {
        this.roles = roles;
    }

    public void addRole(String clazzName, String role) {
        if(role!=null){
            String[] roles = role.split(",");
            for(String r : roles){
                this.roles.put(clazzName, r);
            }

        }
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
