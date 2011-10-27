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
