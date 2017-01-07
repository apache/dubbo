package com.alibaba.dubbo.rpc.protocol.springmvc.exception;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Created by wuyu on 2016/9/15.
 */
public class SpringMvcExceptionHandler implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        JSONObject view = new JSONObject();
        view.put("message", ex.toString());
        view.put("status", 500);
        try {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(view.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
