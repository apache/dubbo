/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.tri.rest.cors;

import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.message.DefaultHttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CorsProcessorTest {

    private HttpRequest request;

    private HttpResponse response;

    private CorsProcessor processor;

    private CorsMeta conf;

    @BeforeEach
    public void setup() {
        this.request = Mockito.mock(HttpRequest.class);
        Mockito.when(this.request.uri()).thenReturn("/test.html");
        Mockito.when(this.request.serverName()).thenReturn("domain1.example");
        Mockito.when(this.request.scheme()).thenReturn("http");
        Mockito.when(this.request.serverPort()).thenReturn(80);
        Mockito.when(this.request.remoteHost()).thenReturn("127.0.0.1");
        this.conf = new CorsMeta();
        this.response = new DefaultHttpResponse();
        this.response.setStatus(HttpStatus.OK.getCode());
        this.processor = new CorsProcessor();
    }

    @Test
    void requestWithoutOriginHeader() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void sameOriginRequest() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("http://domain1.example");
        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void actualRequestWithOriginHeader() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.FORBIDDEN.getCode(), this.response.status());
    }

    @Test
    void actualRequestWithOriginHeaderAndNullConfig() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");

        this.processor.process(null, this.request, this.response);
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void actualRequestWithOriginHeaderAndAllowedOrigin() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        this.conf.addAllowedOrigin("*");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals("*", this.response.header(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_MAX_AGE));
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_EXPOSE_HEADERS));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void actualRequestCredentials() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        this.conf.addAllowedOrigin("https://domain1.com");
        this.conf.addAllowedOrigin("https://domain2.com");
        this.conf.addAllowedOrigin("http://domain3.example");
        this.conf.setAllowCredentials(true);

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals("https://domain2.com", this.response.header(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        Assertions.assertEquals("true", this.response.header(RestConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void actualRequestCredentialsWithWildcardOrigin() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");

        this.conf.addAllowedOrigin("*");
        this.conf.setAllowCredentials(true);
        Assertions.assertFalse(this.processor.process(this.conf, this.request, this.response));

        this.response = new DefaultHttpResponse();
        this.response.setStatus(HttpStatus.OK.getCode());
        this.conf.setAllowedOrigins(null);
        this.conf.addAllowedOriginPattern("*");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals("https://domain2.com", this.response.header(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        Assertions.assertEquals("true", this.response.header(RestConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void actualRequestCaseInsensitiveOriginMatch() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        this.conf.addAllowedOrigin("https://DOMAIN2.com");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void actualRequestTrailingSlashOriginMatch() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        this.conf.addAllowedOrigin("https://domain2.com");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void actualRequestExposedHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        this.conf.addExposedHeader("header1");
        this.conf.addExposedHeader("header2");
        this.conf.addAllowedOrigin("https://domain2.com");
        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals("https://domain2.com", this.response.header(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_EXPOSE_HEADERS));
        Assertions.assertTrue(this.response
                .headerValues(RestConstants.ACCESS_CONTROL_EXPOSE_HEADERS)
                .contains("header1"));
        Assertions.assertTrue(this.response
                .headerValues(RestConstants.ACCESS_CONTROL_EXPOSE_HEADERS)
                .contains("header2"));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestAllOriginsAllowed() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        this.conf.addAllowedOrigin("*");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestWrongAllowedMethod() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("DELETE");
        this.conf.addAllowedOrigin("*");
        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.FORBIDDEN.getCode(), this.response.status());
    }

    @Test
    void preflightRequestMatchedAllowedMethod() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        this.conf.addAllowedOrigin("*");
        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
        Assertions.assertArrayEquals(
                new String[] {"GET", "HEAD"},
                this.response
                        .headerValues(RestConstants.ACCESS_CONTROL_ALLOW_METHODS)
                        .toArray());
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
    }

    @Test
    void preflightRequestTestWithOriginButWithoutOtherHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.FORBIDDEN.getCode(), this.response.status());
    }

    @Test
    void preflightRequestWithoutRequestMethod() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn("Header1");
        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.FORBIDDEN.getCode(), this.response.status());
    }

    @Test
    void preflightRequestWithRequestAndMethodHeaderButNoConfig() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn("Header1");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.FORBIDDEN.getCode(), this.response.status());
    }

    @Test
    void preflightRequestValidRequestAndConfig() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn("Header1");
        this.conf.addAllowedOrigin("*");
        this.conf.addAllowedMethod("GET");
        this.conf.addAllowedMethod("PUT");
        this.conf.addAllowedHeader("header1");
        this.conf.addAllowedHeader("header2");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals("*", this.response.header(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_METHODS));
        Assertions.assertArrayEquals(
                new String[] {"GET", "PUT"},
                this.response
                        .headerValues(RestConstants.ACCESS_CONTROL_ALLOW_METHODS)
                        .toArray());
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_MAX_AGE));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestCredentials() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn("Header1");
        this.conf.addAllowedOrigin("https://domain1.com");
        this.conf.addAllowedOrigin("https://domain2.com");
        this.conf.addAllowedOrigin("http://domain3.example");
        this.conf.addAllowedHeader("Header1");
        this.conf.setAllowCredentials(true);

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals("https://domain2.com", this.response.header(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        Assertions.assertEquals("true", this.response.header(RestConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestCredentialsWithWildcardOrigin() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn("Header1");
        this.conf.setAllowedOrigins(Arrays.asList("https://domain1.com", "*", "http://domain3.example"));
        this.conf.addAllowedHeader("Header1");
        this.conf.setAllowCredentials(true);

        Assertions.assertFalse(this.processor.process(this.conf, this.request, this.response));

        this.response = new DefaultHttpResponse();
        this.response.setStatus(HttpStatus.OK.getCode());
        this.conf.setAllowedOrigins(null);
        this.conf.addAllowedOriginPattern("*");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals("https://domain2.com", this.response.header(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestPrivateNetworkWithWildcardOrigin() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn("Header1");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK))
                .thenReturn("true");
        this.conf.setAllowedOrigins(Arrays.asList("https://domain1.com", "*", "http://domain3.example"));
        this.conf.addAllowedHeader("Header1");
        this.conf.setAllowPrivateNetwork(true);

        Assertions.assertFalse(this.processor.process(this.conf, this.request, this.response));

        this.response = new DefaultHttpResponse();
        this.response.setStatus(HttpStatus.OK.getCode());
        this.conf.setAllowedOrigins(null);
        this.conf.addAllowedOriginPattern("*");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK));
        Assertions.assertEquals("https://domain2.com", this.response.header(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestAllowedHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.headerValues(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn(Arrays.asList("Header1", "Header2"));
        this.conf.addAllowedHeader("Header1");
        this.conf.addAllowedHeader("Header2");
        this.conf.addAllowedHeader("Header3");
        this.conf.addAllowedOrigin("https://domain2.com");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_HEADERS));
        Assertions.assertTrue(this.response
                .headerValues(RestConstants.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("Header1"));
        Assertions.assertTrue(this.response
                .headerValues(RestConstants.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("Header2"));
        Assertions.assertFalse(this.response
                .headerValues(RestConstants.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("Header3"));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestAllowsAllHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.headerValues(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn(Arrays.asList("Header1", "Header2"));
        this.conf.addAllowedHeader("*");
        this.conf.addAllowedOrigin("https://domain2.com");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_HEADERS));
        Assertions.assertTrue(this.response
                .headerValues(RestConstants.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("Header1"));
        Assertions.assertTrue(this.response
                .headerValues(RestConstants.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("Header2"));
        Assertions.assertFalse(this.response
                .headerValues(RestConstants.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("*"));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestWithEmptyHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn("");
        this.conf.addAllowedHeader("*");
        this.conf.addAllowedOrigin("https://domain2.com");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_HEADERS));
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestWithNullConfig() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        this.conf.addAllowedOrigin("*");

        this.processor.process(null, this.request, this.response);
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals(HttpStatus.FORBIDDEN.getCode(), this.response.status());
    }

    @Test
    void preventDuplicatedVaryHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        this.response.setHeader(RestConstants.VARY, RestConstants.ORIGIN);
        this.response.setHeader(RestConstants.VARY, RestConstants.ACCESS_CONTROL_REQUEST_METHOD);
        this.response.setHeader(RestConstants.VARY, RestConstants.ACCESS_CONTROL_REQUEST_HEADERS);

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.headerValues(RestConstants.VARY).contains(RestConstants.ORIGIN));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.headerValues(RestConstants.VARY).contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS));
    }

    @Test
    void preflightRequestWithoutAccessControlRequestPrivateNetwork() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        this.conf.addAllowedHeader("*");
        this.conf.addAllowedOrigin("https://domain2.com");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestWithAccessControlRequestPrivateNetworkNotAllowed() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK))
                .thenReturn("true");
        this.conf.addAllowedHeader("*");
        this.conf.addAllowedOrigin("https://domain2.com");

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertFalse(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestWithAccessControlRequestPrivateNetworkAllowed() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(RestConstants.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.header(RestConstants.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK))
                .thenReturn("true");
        this.conf.addAllowedHeader("*");
        this.conf.addAllowedOrigin("https://domain2.com");
        this.conf.setAllowPrivateNetwork(true);

        this.processor.process(this.conf, this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(RestConstants.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }
}
