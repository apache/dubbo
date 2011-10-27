package com.alibaba.dubbo.remoting.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * http invocation handler.
 * 
 * @author william.liangf
 */
public interface HttpProcessor {

    /**
	 * invoke.
	 * 
	 * @param request request.
	 * @param response response.
	 * @throws IOException
	 * @throws ServletException
	 */
    public abstract void invoke(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;

}
