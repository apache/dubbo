/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.springmvc.support;

import feign.Client;
import feign.Request;
import feign.Response;
import feign.Util;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static feign.Util.UTF_8;

/**
 * This module directs Feign's http requests to Apache's
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">HttpClient</a>. Ex.
 * <pre>
 * GitHub github = Feign.builder().client(new ApacheHttpClient()).target(GitHub.class,
 * "https://api.github.com");
 */
/*
 * Based on Square, Inc's Retrofit ApacheClient implementation
 */
public final class ApacheHttpClient implements Client {
  private static final String ACCEPT_HEADER_NAME = "Accept";

  private final HttpClient client;

  public ApacheHttpClient() {
    this(HttpClientBuilder.create().build());
  }

  public ApacheHttpClient(HttpClient client) {
    this.client = client;
  }

  @Override
  public Response execute(Request request, Request.Options options) throws IOException {
    HttpUriRequest httpUriRequest;
    try {
      httpUriRequest = toHttpUriRequest(request, options);
    } catch (URISyntaxException e) {
      throw new IOException("URL '" + request.url() + "' couldn't be parsed into a URI", e);
    }
    HttpResponse httpResponse = client.execute(httpUriRequest);
    return toFeignResponse(httpResponse);
  }

  HttpUriRequest toHttpUriRequest(Request request, Request.Options options) throws
          UnsupportedEncodingException, MalformedURLException, URISyntaxException {
    RequestBuilder requestBuilder = RequestBuilder.create(request.method());

    //per request timeouts
//    RequestConfig requestConfig = RequestConfig
//            .custom()
//            .setConnectTimeout(options.connectTimeoutMillis())
//            .setSocketTimeout(options.readTimeoutMillis())
//            .build();
    //requestBuilder.setConfig(requestConfig);

    URI uri = new URIBuilder(request.url()).build();

    requestBuilder.setUri(uri.getScheme() + "://" + uri.getAuthority() + uri.getRawPath());

    //request query params
    List<NameValuePair> queryParams = URLEncodedUtils.parse(uri, requestBuilder.getCharset().name());
    for (NameValuePair queryParam: queryParams) {
      requestBuilder.addParameter(queryParam);
    }

    //request headers
    boolean hasAcceptHeader = false;
    for (Map.Entry<String, Collection<String>> headerEntry : request.headers().entrySet()) {
      String headerName = headerEntry.getKey();
      if (headerName.equalsIgnoreCase(ACCEPT_HEADER_NAME)) {
        hasAcceptHeader = true;
      }

      if (headerName.equalsIgnoreCase(Util.CONTENT_LENGTH)) {
        // The 'Content-Length' header is always set by the Apache client and it
        // doesn't like us to set it as well.
        continue;
      }

      for (String headerValue : headerEntry.getValue()) {
        requestBuilder.addHeader(headerName, headerValue);
      }
    }
    //some servers choke on the default accept string, so we'll set it to anything
    if (!hasAcceptHeader) {
      requestBuilder.addHeader(ACCEPT_HEADER_NAME, "*/*");
    }

    //request body
    if (request.body() != null) {
      HttpEntity entity = null;
      if (request.charset() != null) {
        ContentType contentType = getContentType(request);
        String content = new String(request.body(), request.charset());
        entity = new StringEntity(content, contentType);
      } else {
        entity = new ByteArrayEntity(request.body());
      }

      requestBuilder.setEntity(entity);
    }

    return requestBuilder.build();
  }

  private ContentType getContentType(Request request) {
    ContentType contentType = ContentType.DEFAULT_TEXT;
    for (Map.Entry<String, Collection<String>> entry : request.headers().entrySet())
    if (entry.getKey().equalsIgnoreCase("Content-Type")) {
      Collection values = entry.getValue();
      if (values != null && !values.isEmpty()) {
        String value = entry.getValue().iterator().next();
        value = value.split(";")[0];
        contentType = ContentType.create(value, request.charset());
        break;
      }
    }
    return contentType;
  }

  Response toFeignResponse(HttpResponse httpResponse) throws IOException {
    StatusLine statusLine = httpResponse.getStatusLine();
    int statusCode = statusLine.getStatusCode();

    String reason = statusLine.getReasonPhrase();

    Map<String, Collection<String>> headers = new HashMap<String, Collection<String>>();
    for (Header header : httpResponse.getAllHeaders()) {
      String name = header.getName();
      String value = header.getValue();

      Collection<String> headerValues = headers.get(name);
      if (headerValues == null) {
        headerValues = new ArrayList<String>();
        headers.put(name, headerValues);
      }
      headerValues.add(value);
    }

    return Response.create(statusCode, reason, headers, toFeignBody(httpResponse));
  }

  Response.Body toFeignBody(HttpResponse httpResponse) throws IOException {
    final HttpEntity entity = httpResponse.getEntity();
    if (entity == null) {
      return null;
    }
    return new Response.Body() {

      @Override
      public Integer length() {
        return entity.getContentLength() >= 0 ? (int) entity.getContentLength() : null;
      }

      @Override
      public boolean isRepeatable() {
        return entity.isRepeatable();
      }

      @Override
      public InputStream asInputStream() throws IOException {
        return entity.getContent();
      }

      @Override
      public Reader asReader() throws IOException {
        return new InputStreamReader(asInputStream(), UTF_8);
      }

      @Override
      public void close() throws IOException {
        EntityUtils.consume(entity);
      }
    };
  }
}
