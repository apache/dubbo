/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.http;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Service dispatcher Servlet.
 * 
 * @author qian.lei
 */
public class ServiceDispatcherServlet extends HttpServlet {

	private static final long serialVersionUID = 5766349180380479888L;

	private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    
    private static final Map<String, HttpProcessor> processors = new ConcurrentHashMap<String, HttpProcessor>();

    public static void addProcessor(int port, String uri, HttpProcessor processor) {
        processors.put(key(port, uri), processor);
    }

    public static void removeProcessor(int port, String uri) {
        processors.remove(key(port, uri));
    }

    private static String key(int port, String uri) {
        return port + ":" + (uri.startsWith("/") ? uri : "/" + uri);
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
        String uri = request.getRequestURI();
        String contentType = request.getContentType();
        if (contentType == null || FORM_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
            int i = uri.lastIndexOf('/');
            if (i >= 0) {
                uri = uri.substring(0, i);
            }
        }
        HttpProcessor processor = processors.get(key(request.getLocalPort(), uri));
        if( processor == null ) {// service not found.
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Service not found.");
        } else {
            processor.invoke(request, response);
        }
    }

}