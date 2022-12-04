package org.apache.dubbo.rpc.protocol.rest.annotation.consumer;

import org.apache.dubbo.rpc.protocol.mvc.constans.RestConstant;

import java.io.Serializable;
import java.util.*;


public class RequestTemplate implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String ACCEPT = "Accept";
    public static final String DEFAULT_ACCEPT = "*/*";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String CONTENT_LENGTH = "Content-Length";
    public static final String ENCODING_GZIP = "gzip";
    public static final String ENCODING_DEFLATE = "deflate";
    private static final List<String> EMPTY_ARRAYLIST = new ArrayList<>();

    private final Map<String, Collection<String>> queries = new LinkedHashMap<String, Collection<String>>();
    private final Map<String, Collection<String>> headers = new LinkedHashMap<String, Collection<String>>();
    private String httpMethod;
    private String path;
    private Object body;
    private byte[] byteBody;

    public RequestTemplate() {
        addHeader(ACCEPT, DEFAULT_ACCEPT);
    }

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


    public RequestTemplate path(String path) {
        this.path = path;
        return this;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public RequestTemplate httpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public byte[] getSerializedBody() {
        return byteBody;
    }

    public void serializeBody(byte[] body) {
        this.byteBody = body;
    }

    public boolean isBodyEmpty() {
        return getSerializedBody() == null;
    }

    public RequestTemplate body(Object body) {
        this.body = body;
        return this;
    }

    public Object getUnSerializedBody() {
        return body;
    }

    public Map<String, Collection<String>> getAllHeaders() {
        return headers;
    }

    public Collection<String> getHeader(String name) {
        return headers.get(name);
    }

    public Collection<String> getEncodingValues() {
        if (headers.containsKey(CONTENT_ENCODING)) {
            return headers.get(CONTENT_ENCODING);
        }
        return EMPTY_ARRAYLIST;
    }

    public boolean isGzipEncodedRequest() {
        return getEncodingValues().contains(ENCODING_GZIP);
    }

    public boolean isDeflateEncodedRequest() {
        return getEncodingValues().contains(ENCODING_DEFLATE);
    }

    public void addHeader(String key, String value) {
        addValueByKey(key, value, this.headers);
    }

    public void addHeader(String key, Object value) {
        addValueByKey(key, String.valueOf(value), this.headers);
    }

    public void addParam(String key, String value) {
        addValueByKey(key, value, this.queries);
    }


    public void addValueByKey(String key, String value, Map<String, Collection<String>> maps) {

        if (value == null) {
            return;
        }

        Collection<String> values = null;
        if (!maps.containsKey(key)) {
            values = new HashSet<>();
        } else {
            values = maps.get(key);
        }

        values.add(value);

    }


    public Integer getContentLength() {

        if (!getAllHeaders().containsKey(CONTENT_LENGTH)) {
            return null;
        }

        HashSet<String> strings = (HashSet<String>) getAllHeaders().get(CONTENT_LENGTH);

        return Integer.parseInt(new ArrayList<>(strings).get(0));

    }

    public byte getSerializeId() {
        return Byte.parseByte(getHeader(RestConstant.SERIALIZATION_KEY).toArray(new String[0])[0]);
    }


}
