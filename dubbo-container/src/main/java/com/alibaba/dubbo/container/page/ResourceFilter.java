/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-5-26
 * 
 * Copyright 1999-2010 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.container.page;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.dubbo.common.Constants;

/**
 * ResourceServlet
 * 
 * @author william.liangf
 */
public class ResourceFilter implements Filter {

    private static final String CLASSPATH_PREFIX = "classpath:";

    private final long start = System.currentTimeMillis();

    private final List<String> resources = new ArrayList<String>();

    public void init(FilterConfig filterConfig) throws ServletException {
        String config = filterConfig.getInitParameter("resources");
        if (config != null && config.length() > 0) {
            String[] configs = Constants.COMMA_SPLIT_PATTERN.split(config);
            for (String c : configs) {
                if (c != null && c.length() > 0) {
                    c = c.replace('\\', '/');
                    if (c.endsWith("/")) {
                        c = c.substring(0, c.length() - 1);
                    }
                    resources.add(c);
                }
            }
        }
    }

    public void destroy() {
    }
    
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if (response.isCommitted()) {
            return;
        }
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (uri.endsWith("/favicon.ico")) {
            uri = "/favicon.ico";
        } else if (context != null && ! "/".equals(context)) {
            uri = uri.substring(context.length());
        }
        if (! uri.startsWith("/")) {
            uri = "/" + uri;
        }
        long lastModified = getLastModified(uri);
        long since = request.getDateHeader("If-Modified-Since");
        if (since >= lastModified) {
        	response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        	return;
        }
        byte[] data;
        InputStream input = getInputStream(uri);
    	if (input == null) {
    	    chain.doFilter(req, res);
            return;
        }
    	try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            data = output.toByteArray();
        } finally {
            input.close();
        }
        response.setDateHeader("Last-Modified", lastModified);
        OutputStream output = response.getOutputStream();
        output.write(data);
        output.flush();
    }
    
    private boolean isFile(String path) {
        return path.startsWith("/") || path.indexOf(":") <= 1;
    }
	
	private long getLastModified(String uri) {
	    for (String resource : resources) {
            if (resource != null && resource.length() > 0) {
                String path = resource + uri;
                if (isFile(path)) {
                    File file = new File(path);
                    if (file.exists()) {
                        return file.lastModified();
                    }
                }
            }
        }
        return start;
	}
	
	private InputStream getInputStream(String uri) {
        for (String resource : resources) {
            String path = resource + uri;
            try {
                if (isFile(path)) {
                    return new FileInputStream(path);
                } else if (path.startsWith(CLASSPATH_PREFIX)) {
                    return Thread.currentThread().getContextClassLoader().getResourceAsStream(path.substring(CLASSPATH_PREFIX.length()));
                } else {
                    return new URL(path).openStream();
                }
            } catch (IOException e) {
            }
        }
        return null;
	}

}
