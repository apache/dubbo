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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ResourceServlet
 * 
 * @author william.liangf
 */
public class ResourceServlet extends HttpServlet {

    private static final long serialVersionUID = -8315200468240234179L;
    
    private static final String CLASSPATH_PREFIX = "classpath:";

    private final long start;

    private final Map<String, String> resourceMap = new ConcurrentHashMap<String, String>();

    private final Map<String, byte[]> cacheMap = new ConcurrentHashMap<String, byte[]>();

	public ResourceServlet() {
		start = System.currentTimeMillis();
	}

	public void setResources(String[] resources) {
	    if (resources != null && resources.length > 0) {
	        for (String resource : resources) {
	            resource = resource.replace('\\', '/');
                int i = resource.lastIndexOf('/');
                String name;
                if (i >= 0) {
                    name = resource.substring(i + 1);
                } else {
                    name = resource;
                }
                resourceMap.put(name, resource);
	        }
	    }
	}

    @Override
    protected final void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

	@Override
    protected final void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
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
        long since = request.getDateHeader("If-Modified-Since");
        if (since >= start) {
        	response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        	return;
        }
        byte[] data = cacheMap.get(uri);
        if (data == null) {
            InputStream input = getInputStream(uri);
        	if (input == null) {
            	response.sendError(HttpServletResponse.SC_NOT_FOUND);
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
                cacheMap.put(uri, data);
            } finally {
                input.close();
            }
        }
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
        OutputStream output = response.getOutputStream();
        output.write(data);
        output.flush();
    }
	
	private InputStream getInputStream(String uri) {
	    int i = uri.indexOf('/', 1);
	    if (i >= 0) {
    	    String name = uri.substring(1, i);
    	    uri = uri.substring(i);
    	    String resource = resourceMap.get(name);
    	    if (resource != null && resource.length() > 0) {
    	        String path = resource + uri;
    	        try {
        	        if (path.startsWith(CLASSPATH_PREFIX)) {
        	            return Thread.currentThread().getContextClassLoader().getResourceAsStream(path.substring(CLASSPATH_PREFIX.length()));
        	        } else if (path.indexOf(":") > 0) {
        	            return new URL(path).openStream();
        	        } else {
        	            return new FileInputStream(path);
        	        }
    	        } catch (IOException e) {
                }
    	    }
	    }
        return null;
	}

}
