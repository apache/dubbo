package org.apache.dubbo.rpc.protocol.rest.annotation.consumer;

import org.apache.dubbo.metadata.rest.RequestMetadata;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


public class RequestTemplate implements Serializable {
    private static final AtomicLong REQUEST_ID = new AtomicLong();
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
    private String address;
    private Object body;
    private byte[] byteBody;
    private String protocol = "http://";


    public RequestTemplate(String httpMethod, String address) {
        this.httpMethod = httpMethod;
        this.address = address;
    }

    public String getURL() {
        StringBuilder stringBuilder = new StringBuilder(getProtocol() + address);

        stringBuilder.append(getUri());
        return stringBuilder.toString();
    }

    public String getUri() {
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
        addHeader(CONTENT_LENGTH, body.length); // must header
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

    public void addKeepAliveHeader(int time) {
        addHeader(RestConstant.KEEP_ALIVE_HEADER, time);
        addHeader(RestConstant.CONNECTION, RestConstant.KEEP_ALIVE);
    }

    public void addHeaders(String key, Collection<String> values) {
        Collection<String> header = getHeader(key);

        if (header == null) {
            header = new HashSet<>();
            this.headers.put(key, header);
        }
        header.addAll(values);
    }


    public void addParam(String key, String value) {
        addValueByKey(key, value, this.queries);
    }

    public void addParam(String key, Object value) {
        addParam(key, String.valueOf(value));
    }

    public Map<String, Collection<String>> getQueries() {
        return queries;
    }

    public Collection<String> getParam(String key) {
        return getQueries().get(key);
    }

    public void addParams(String key, Collection<String> values) {
        Collection<String> params = getParam(key);

        if (params == null) {
            params = new HashSet<>();
            this.headers.put(key, params);
        }
        params.addAll(values);
    }


    public void addValueByKey(String key, String value, Map<String, Collection<String>> maps) {

        if (value == null) {
            return;
        }

        Collection<String> values = null;
        if (!maps.containsKey(key)) {
            values = new HashSet<>();
            maps.put(key, values);
        }
        values = maps.get(key);


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


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        addHeader("Host", address);// must header
        this.address = address;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean emptyBody() {
        return getUnSerializedBody() == null;
    }
}
