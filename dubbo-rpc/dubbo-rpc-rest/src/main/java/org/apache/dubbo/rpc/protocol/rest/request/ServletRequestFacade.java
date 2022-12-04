package org.apache.dubbo.rpc.protocol.rest.request;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Map;

public abstract class ServletRequestFacade<T> {

    protected T request;

    public ServletRequestFacade(T request) {
        this.request = request;
    }

    public T getRequest() {
        return request;
    }

    public abstract Object getParts() throws Exception;

    public abstract Object getPart(String var1) throws Exception;

    public abstract Object getCookies();

    public abstract long getDateHeader(String name);


    public abstract String getHeader(String name);


    public abstract Enumeration<String> getHeaders(String name);


    public abstract Enumeration<String> getHeaderNames();


    public abstract int getIntHeader(String name);


    public abstract String getMethod();


    public abstract String getPathInfo();


    public abstract String getPathTranslated();


    public abstract String getContextPath();


    public abstract String getQueryString();


    public abstract String getRemoteUser();


    public abstract boolean isUserInRole(String role);


    public abstract String getRequestedSessionId();


    public abstract String getRequestURI();


    public abstract StringBuffer getRequestURL();


    public abstract String getServletPath();


    public abstract String changeSessionId();


    public abstract boolean isRequestedSessionIdValid();


    public abstract boolean isRequestedSessionIdFromCookie();


    public abstract boolean isRequestedSessionIdFromURL();


    public abstract boolean isRequestedSessionIdFromUrl();

    public abstract Object getAttribute(String name);


    public abstract Enumeration<String> getAttributeNames();


    public abstract String getCharacterEncoding();


    public abstract void setCharacterEncoding(String env) throws UnsupportedEncodingException;


    public abstract int getContentLength();


    public abstract long getContentLengthLong();


    public abstract String getContentType();


    public abstract String getParameter(String name);


    public abstract Enumeration<String> getParameterNames();


    public abstract String[] getParameterValues(String name);


    public abstract Map<String, String[]> getParameterMap();


    public abstract String getProtocol();


    public abstract String getScheme();


    public abstract String getServerName();


    public abstract int getServerPort();


    public abstract BufferedReader getReader() throws IOException;


    public abstract String getRemoteAddr();


    public abstract String getRemoteHost();


    public abstract void setAttribute(String name, Object o);


    public abstract void removeAttribute(String name);


    public abstract boolean isSecure();


    public abstract String getRealPath(String path);


    public abstract int getRemotePort();


    public abstract String getLocalName();


    public abstract String getLocalAddr();


    public abstract int getLocalPort();

    public abstract InputStream getInputStream() throws IOException;


}
