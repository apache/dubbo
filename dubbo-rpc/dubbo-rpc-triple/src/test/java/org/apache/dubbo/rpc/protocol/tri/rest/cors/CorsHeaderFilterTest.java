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
import org.apache.dubbo.remoting.http12.exception.HttpResultPayloadException;
import org.apache.dubbo.remoting.http12.message.DefaultHttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.CorsMeta;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CorsHeaderFilterTest {

    private static final Logger log = LoggerFactory.getLogger(CorsHeaderFilterTest.class);
    private HttpRequest request;

    private HttpResponse response;

    private MockCorsHeaderFilter processor;

    private RequestMapping build;

    class MockCorsHeaderFilter extends CorsHeaderFilter {
        public void process(HttpRequest request, HttpResponse response) {
            invoke(null, null, request, response);
        }
    }

    private CorsMeta defaultCorsMeta() {
        return CorsMeta.builder().maxAge((long) 1000.0).build();
    }

    @BeforeEach
    public void setup() {
        build = Mockito.mock(RequestMapping.class);
        this.request = Mockito.mock(HttpRequest.class);
        Mockito.when(this.request.attribute(RestConstants.MAPPING_ATTRIBUTE)).thenReturn(build);
        Mockito.when(this.request.uri()).thenReturn("/test.html");
        Mockito.when(this.request.serverName()).thenReturn("domain1.example");
        Mockito.when(this.request.scheme()).thenReturn("http");
        Mockito.when(this.request.serverPort()).thenReturn(80);
        Mockito.when(this.request.remoteHost()).thenReturn("127.0.0.1");
        this.response = new DefaultHttpResponse();
        this.response.setStatus(HttpStatus.OK.getCode());
        this.processor = new MockCorsHeaderFilter();
    }

    @Test
    void requestWithoutOriginHeader() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(build.getCors()).thenReturn(CorsMeta.builder().build());
        Mockito.when(build.getCors()).thenReturn(defaultCorsMeta());
        this.processor.process(this.request, this.response);
        Assertions.assertFalse(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ORIGIN));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void sameOriginRequest() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("http://domain1.example");
        Mockito.when(build.getCors()).thenReturn(defaultCorsMeta());
        this.processor.process(this.request, this.response);
        Assertions.assertFalse(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ORIGIN));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void actualRequestWithOriginHeader() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(build.getCors()).thenReturn(defaultCorsMeta());
        Assertions.assertThrows(HttpResultPayloadException.class, () -> {
            this.processor.process(this.request, this.response);
        });
    }

    @Test
    void actualRequestWithOriginHeaderAndNullConfig() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(build.getCors()).thenReturn(null);
        this.processor.process(this.request, this.response);
        Assertions.assertFalse(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void actualRequestWithOriginHeaderAndAllowedOrigin() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(build.getCors())
                .thenReturn(CorsMeta.builder().allowedOrigins("*").build());
        this.processor.process(this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals("*", this.response.header(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertFalse(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_MAX_AGE));
        Assertions.assertFalse(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_EXPOSE_HEADERS));
        Assertions.assertTrue(this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ORIGIN));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void actualRequestCaseInsensitiveOriginMatch() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(build.getCors())
                .thenReturn(
                        CorsMeta.builder().allowedOrigins("https://DOMAIN2.com").build());
        this.processor.process(this.request, this.response);
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void actualRequestTrailingSlashOriginMatch() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(build.getCors())
                .thenReturn(CorsMeta.builder()
                        .allowedOrigins("https://domain2.com/")
                        .build());
        this.processor.process(this.request, this.response);
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void actualRequestExposedHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.doReturn(CorsMeta.builder()
                        .allowedOrigins("https://domain2.com")
                        .exposedHeaders("header1", "header2")
                        .build())
                .when(build)
                .getCors();
        this.processor.process(this.request, this.response);
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals(
                "https://domain2.com", this.response.header(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_EXPOSE_HEADERS));
        Assertions.assertTrue(this.response
                .header(CorsHeaderFilter.ACCESS_CONTROL_EXPOSE_HEADERS)
                .contains("header1"));
        Assertions.assertTrue(this.response
                .header(CorsHeaderFilter.ACCESS_CONTROL_EXPOSE_HEADERS)
                .contains("header2"));
        Assertions.assertTrue(this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ORIGIN));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestWithoutRequestheader() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn(true);
        Mockito.when(build.getCors())
                .thenReturn(CorsMeta.builder().allowedOrigins("*").build());
        try {
            this.processor.process(this.request, this.response);
            Assertions.fail();
        } catch (HttpResultPayloadException e) {
            Assertions.assertEquals(HttpStatus.NO_CONTENT.getCode(), e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void preflightRequestWrongAllowedMethod() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("DELETE");
        Mockito.when(request.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn(true);
        Mockito.when(build.getCors())
                .thenReturn(CorsMeta.builder().allowedOrigins("*").build());
        try {
            this.processor.process(this.request, this.response);
            Assertions.fail();
        } catch (HttpResultPayloadException e) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN.getCode(), e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void preflightRequestMatchedAllowedMethod() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn(true);
        Mockito.when(build.getCors())
                .thenReturn(CorsMeta.builder().allowedOrigins("*").build());
        try {
            this.processor.process(this.request, this.response);
            Assertions.fail();
        } catch (HttpResultPayloadException e) {
            Assertions.assertEquals(HttpStatus.NO_CONTENT.getCode(), e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void preflightRequestTestWithOriginButWithoutOtherHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn(true);
        Mockito.when(build.getCors()).thenReturn(defaultCorsMeta());
        try {
            this.processor.process(this.request, this.response);
            Assertions.fail();
        } catch (HttpResultPayloadException e) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN.getCode(), e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void preflightRequestWithoutRequestMethod() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn("Header1");
        Mockito.when(request.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn(true);
        Mockito.when(build.getCors()).thenReturn(defaultCorsMeta());
        try {
            this.processor.process(this.request, this.response);
            Assertions.fail();
        } catch (HttpResultPayloadException e) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN.getCode(), e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void preflightRequestWithRequestAndMethodHeaderButNoConfig() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn("Header1");
        Mockito.when(request.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn(true);
        Mockito.when(build.getCors()).thenReturn(defaultCorsMeta());
        try {
            this.processor.process(this.request, this.response);
            Assertions.fail();
        } catch (HttpResultPayloadException e) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN.getCode(), e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void preflightRequestValidRequestAndConfig() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn("Header1");
        Mockito.when(request.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn(true);
        Mockito.when(build.getCors())
                .thenReturn(CorsMeta.builder()
                        .allowedOrigins("*")
                        .allowedMethods("GET", "PUT")
                        .allowedHeaders("Header1", "Header2")
                        .build());
        try {
            this.processor.process(this.request, this.response);
            Assertions.fail();
        } catch (HttpResultPayloadException e) {
            Assertions.assertEquals(HttpStatus.NO_CONTENT.getCode(), e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail();
        }
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertEquals("*", this.response.header(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_METHODS));
        log.info("{}", this.response.headerValues(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_METHODS));
        Assertions.assertArrayEquals(
                new String[] {"GET"},
                this.response
                        .headerValues(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_METHODS)
                        .toArray());
        Assertions.assertFalse(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_MAX_AGE));
        Assertions.assertTrue(this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ORIGIN));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS));
    }

    @Test
    void preflightRequestAllowedHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn(true);
        Mockito.doReturn(Arrays.asList("Header1", "Header2"))
                .when(request)
                .headerValues(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS);
        Mockito.doReturn(CorsMeta.builder()
                        .allowedOrigins("https://domain2.com")
                        .allowedHeaders("Header1", "Header2")
                        .build())
                .when(build)
                .getCors();
        try {
            this.processor.process(this.request, this.response);
            Assertions.fail();
        } catch (HttpResultPayloadException e) {
            Assertions.assertEquals(HttpStatus.NO_CONTENT.getCode(), e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail();
        }
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_HEADERS));
        Assertions.assertTrue(this.response
                .header(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("Header1"));
        Assertions.assertTrue(this.response
                .header(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("Header2"));
        Assertions.assertFalse(this.response
                .header(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("Header3"));
        Assertions.assertTrue(this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ORIGIN));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestAllowsAllHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");

        Mockito.when(request.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn(true);
        Mockito.when(request.headerValues(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn(Arrays.asList("Header1", "Header2"));
        Mockito.when(build.getCors())
                .thenReturn(CorsMeta.builder()
                        .allowedOrigins("https://domain2.com")
                        .allowedHeaders("*")
                        .build());
        try {
            this.processor.process(this.request, this.response);
            Assertions.fail();
        } catch (HttpResultPayloadException e) {
            Assertions.assertEquals(HttpStatus.NO_CONTENT.getCode(), e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail();
        }
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_HEADERS));
        Assertions.assertTrue(this.response
                .header(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("Header1"));
        Assertions.assertTrue(this.response
                .header(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("Header2"));
        Assertions.assertFalse(this.response
                .header(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_HEADERS)
                .contains("*"));
        Assertions.assertTrue(this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ORIGIN));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS));
    }

    @Test
    void preflightRequestWithEmptyHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(request.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn(true);
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS))
                .thenReturn("");
        Mockito.when(build.getCors())
                .thenReturn(CorsMeta.builder()
                        .allowedOrigins("https://domain2.com")
                        .allowedHeaders("*")
                        .build());
        try {
            this.processor.process(this.request, this.response);
            Assertions.fail();
        } catch (HttpResultPayloadException e) {
            Assertions.assertEquals(HttpStatus.NO_CONTENT.getCode(), e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail();
        }
        Assertions.assertTrue(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        Assertions.assertFalse(this.response.hasHeader(CorsHeaderFilter.ACCESS_CONTROL_ALLOW_HEADERS));
        Assertions.assertTrue(this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ORIGIN));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS));
        Assertions.assertEquals(HttpStatus.OK.getCode(), this.response.status());
    }

    @Test
    void preflightRequestWithNullConfig() {
        Mockito.when(request.method()).thenReturn(HttpMethods.OPTIONS.name());
        Mockito.when(request.header(CorsHeaderFilter.ORIGIN)).thenReturn("https://domain2.com");
        Mockito.when(request.header(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD))
                .thenReturn("GET");
        Mockito.when(build.getCors())
                .thenReturn(CorsMeta.builder().allowedOrigins("*").build());
        try {
            this.processor.process(this.request, this.response);
            Assertions.fail();
        } catch (HttpResultPayloadException e) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN.getCode(), e.getStatusCode());
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void preventDuplicatedVaryHeaders() {
        Mockito.when(request.method()).thenReturn(HttpMethods.GET.name());
        this.response.setHeader(
                CorsHeaderFilter.VARY,
                CorsHeaderFilter.ORIGIN + "," + CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD + ","
                        + CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS);
        this.processor.process(this.request, this.response);
        Assertions.assertTrue(this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ORIGIN));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_METHOD));
        Assertions.assertTrue(
                this.response.header(CorsHeaderFilter.VARY).contains(CorsHeaderFilter.ACCESS_CONTROL_REQUEST_HEADERS));
    }
}
