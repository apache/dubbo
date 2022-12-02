package org.apache.dubbo.rpc.protocol.mvc.annotation.consumer;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


public class RequestTemplate implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, Collection<String>> queries = new LinkedHashMap<String, Collection<String>>();
    private final Map<String, Collection<String>> headers = new LinkedHashMap<String, Collection<String>>();
    private String httpMethod;
    private String path;
    private byte[] body;


    public String getRequestLine() {
        StringBuilder stringBuilder = new StringBuilder(path);
        return stringBuilder.append(getQueryString()).toString();
    }

    public String getQueryString() {

        if (queries.isEmpty()) {
            return "";
        }

        StringBuilder queryBuilder = new StringBuilder("?");
        for (String field : queries.keySet()) {

            Collection<String> queryValues = queries.get(field);

            if (queryValues == null || queryValues.isEmpty()) {
                continue;
            }

            for (String value : queryValues) {
                queryBuilder.append('&');
                queryBuilder.append(field);
                if (value == null) {
                    continue;
                }

                queryBuilder.append('=');
                queryBuilder.append(value);
            }
        }

        return queryBuilder.toString();

    }


    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }


}
