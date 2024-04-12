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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CorsMetaTest {
    @Test
    void setNullValues() {
        CorsMeta config = new CorsMeta();
        config.setAllowedOrigins(null);
        config.setAllowedOriginPatterns(null);
        config.setAllowedHeaders(null);
        config.setAllowedMethods(null);
        config.setExposedHeaders(null);
        config.setAllowCredentials(null);
        config.setMaxAge(null);
        config.setAllowPrivateNetwork(null);
        Assertions.assertNull(config.getAllowedOrigins());
        Assertions.assertNull(config.getAllowedOriginPatterns());
        Assertions.assertNull(config.getAllowedHeaders());
        Assertions.assertNull(config.getAllowedMethods());
        Assertions.assertNull(config.getExposedHeaders());
        Assertions.assertNull(config.getAllowCredentials());
        Assertions.assertNull(config.getAllowPrivateNetwork());
        Assertions.assertNull(config.getMaxAge());
    }

    @Test
    void setValues() {
        CorsMeta config = new CorsMeta();

        // Add allowed origin
        config.addAllowedOrigin("*");
        config.addAllowedOriginPattern("http://*.example.com");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("*");
        config.setAllowCredentials(true);
        config.setAllowPrivateNetwork(true);
        config.setMaxAge(123L);
        Assertions.assertArrayEquals(
                new String[] {"*"}, config.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"http://*.example.com"},
                config.getAllowedOriginPatterns().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, config.getAllowedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, config.getAllowedMethods().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, config.getExposedHeaders().toArray());
        Assertions.assertTrue(config.getAllowCredentials());
        Assertions.assertTrue(config.getAllowPrivateNetwork());
        Assertions.assertEquals(123L, config.getMaxAge().longValue());
    }

    @Test
    void combineWithNull() {

        CorsMeta config = new CorsMeta();
        config.setAllowedOrigins(Collections.singletonList("*"));
        CorsMeta.combine(config, null);
        Assertions.assertArrayEquals(
                new String[] {"*"}, config.getAllowedOrigins().toArray());
        Assertions.assertNull(config.getAllowedOriginPatterns());
    }

    @Test
    void combineWithConfigWithNullProperties() {
        CorsMeta config = new CorsMeta();
        config.addAllowedOrigin("*");
        config.setAllowedOriginPatterns(Collections.singletonList("http://*.example.com"));
        config.addAllowedHeader("header1");
        config.addExposedHeader("header3");
        config.addAllowedMethod(HttpMethods.GET.name());
        config.setMaxAge(123L);
        config.setAllowCredentials(true);
        config.setAllowPrivateNetwork(true);
        CorsMeta other = new CorsMeta();
        config = CorsMeta.combine(config, other);
        // Assert the combined config
        Assertions.assertNotNull(config);
        Assertions.assertArrayEquals(
                new String[] {"*"}, config.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"http://*.example.com"},
                config.getAllowedOriginPatterns().toArray());
        Assertions.assertArrayEquals(
                new String[] {"header1"}, config.getAllowedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {"header3"}, config.getExposedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {HttpMethods.GET.name()},
                config.getAllowedMethods().toArray());
        Assertions.assertEquals(123L, config.getMaxAge().longValue());
        Assertions.assertTrue(config.getAllowCredentials());
        Assertions.assertTrue(config.getAllowPrivateNetwork());
    }

    @Test
    void combineWithDefaultPermitValues() {
        CorsMeta priority = new CorsMeta().applyPermitDefaultValues();
        CorsMeta other = new CorsMeta();
        other.addAllowedOrigin("https://domain.com");
        other.addAllowedHeader("header1");
        other.addAllowedMethod(HttpMethods.PUT.name());
        CorsMeta combinedConfig = CorsMeta.combine(priority, other);

        Assertions.assertNotNull(combinedConfig);
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getAllowedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {HttpMethods.GET.name(), HttpMethods.HEAD.name(), HttpMethods.POST.name()},
                combinedConfig.getAllowedMethods().toArray());
        Assertions.assertTrue(combinedConfig.getExposedHeaders().isEmpty());

        combinedConfig = CorsMeta.combine(other, priority);
        Assertions.assertNotNull(combinedConfig);
        Assertions.assertArrayEquals(
                new String[] {"https://domain.com"},
                combinedConfig.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"header1"}, combinedConfig.getAllowedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {HttpMethods.PUT.name()},
                combinedConfig.getAllowedMethods().toArray());
        Assertions.assertTrue(combinedConfig.getExposedHeaders().isEmpty());
        combinedConfig = CorsMeta.combine(priority, new CorsMeta());
        Assertions.assertNotNull(combinedConfig);
        Assertions.assertArrayEquals(
                new String[] {"*"}, priority.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, priority.getAllowedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {HttpMethods.GET.name(), HttpMethods.HEAD.name(), HttpMethods.POST.name()},
                combinedConfig.getAllowedMethods().toArray());
        Assertions.assertTrue(combinedConfig.getExposedHeaders().isEmpty());

        // Combine an empty config with config
        combinedConfig = CorsMeta.combine(new CorsMeta(), priority);

        // Assert the combined config
        Assertions.assertNotNull(combinedConfig);
        Assertions.assertArrayEquals(
                new String[] {"*"}, priority.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, priority.getAllowedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {HttpMethods.GET.name(), HttpMethods.HEAD.name(), HttpMethods.POST.name()},
                combinedConfig.getAllowedMethods().toArray());
        Assertions.assertTrue(combinedConfig.getExposedHeaders().isEmpty());
    }

    @Test
    void combinePatternWithDefaultPermitValues() {
        // Create a config with default permit values
        CorsMeta config = new CorsMeta().applyPermitDefaultValues();

        // Create another config with an allowed origin pattern
        CorsMeta other = new CorsMeta();
        other.addAllowedOriginPattern("http://*.com");

        // Combine the configs, with 'other' first
        CorsMeta combinedConfig = CorsMeta.combine(other, config);

        // Assert the combined config
        Assertions.assertNotNull(combinedConfig);
        Assertions.assertNull(combinedConfig.getAllowedOrigins());
        Assertions.assertArrayEquals(
                new String[] {"http://*.com"},
                combinedConfig.getAllowedOriginPatterns().toArray());

        // Combine the configs, with 'config' first
        combinedConfig = CorsMeta.combine(config, other);

        // Assert the combined config
        Assertions.assertNotNull(combinedConfig);
        Assertions.assertNull(combinedConfig.getAllowedOrigins());
        Assertions.assertArrayEquals(
                new String[] {"http://*.com"},
                combinedConfig.getAllowedOriginPatterns().toArray());
    }

    @Test
    void combinePatternWithDefaultPermitValuesAndCustomOrigin() {
        // Create a config with default permit values and a custom allowed origin
        CorsMeta config = new CorsMeta().applyPermitDefaultValues();
        config.setAllowedOrigins(Collections.singletonList("https://domain.com"));

        // Create another config with an allowed origin pattern
        CorsMeta other = new CorsMeta();
        other.addAllowedOriginPattern("http://*.com");

        // Combine the configs, with 'other' first
        CorsMeta combinedConfig = CorsMeta.combine(other, config);

        // Assert the combined config
        Assertions.assertNotNull(combinedConfig);
        Assertions.assertArrayEquals(
                new String[] {"https://domain.com"},
                combinedConfig.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"http://*.com"},
                combinedConfig.getAllowedOriginPatterns().toArray());

        // Combine the configs, with 'config' first
        combinedConfig = CorsMeta.combine(config, other);

        // Assert the combined config
        Assertions.assertNotNull(combinedConfig);
        Assertions.assertArrayEquals(
                new String[] {"https://domain.com"},
                combinedConfig.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"http://*.com"},
                combinedConfig.getAllowedOriginPatterns().toArray());
    }

    @Test
    void combineWithAsteriskWildCard() {
        // Create a config with wildcard values
        CorsMeta config = new CorsMeta();
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("*");
        config.addAllowedMethod("*");
        config.addAllowedOriginPattern("*");

        // Create another config with some custom values
        CorsMeta other = new CorsMeta();
        other.addAllowedOrigin("https://domain.com");
        other.addAllowedOriginPattern("http://*.company.com");
        other.addAllowedHeader("header1");
        other.addExposedHeader("header2");
        other.addAllowedHeader("anotherHeader1");
        other.addExposedHeader("anotherHeader2");
        other.addAllowedMethod(HttpMethods.PUT.name());

        // Combine the configs, with 'config' first
        CorsMeta combinedConfig = CorsMeta.combine(config, other);

        // Assert the combined config
        Assertions.assertNotNull(combinedConfig);
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getAllowedOriginPatterns().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getAllowedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getExposedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getAllowedMethods().toArray());

        // Combine the configs, with 'other' first
        combinedConfig = CorsMeta.combine(other, config);

        // Assert the combined config
        Assertions.assertNotNull(combinedConfig);
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getAllowedOriginPatterns().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getAllowedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getExposedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*"}, combinedConfig.getAllowedMethods().toArray());
    }

    @Test
    void combineWithDuplicatedElements() {
        // Create a config with some duplicate values
        CorsMeta config = new CorsMeta();
        config.addAllowedOrigin("https://domain1.com");
        config.addAllowedOrigin("https://domain2.com");
        config.addAllowedHeader("header1");
        config.addAllowedHeader("header2");
        config.addExposedHeader("header3");
        config.addExposedHeader("header4");
        config.addAllowedMethod(HttpMethods.GET.name());
        config.addAllowedMethod(HttpMethods.PUT.name());
        config.addAllowedOriginPattern("http://*.domain1.com");
        config.addAllowedOriginPattern("http://*.domain2.com");

        // Create another config with some overlapping values
        CorsMeta other = new CorsMeta();
        other.addAllowedOrigin("https://domain1.com");
        other.addAllowedOriginPattern("http://*.domain1.com");
        other.addAllowedHeader("header1");
        other.addExposedHeader("header3");
        other.addAllowedMethod(HttpMethods.GET.name());

        // Combine the configs
        CorsMeta combinedConfig = CorsMeta.combine(config, other);

        // Assert the combined config
        Assertions.assertNotNull(combinedConfig);
        Assertions.assertArrayEquals(
                new String[] {"https://domain1.com", "https://domain2.com"},
                combinedConfig.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"header1", "header2"},
                combinedConfig.getAllowedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {"header3", "header4"},
                combinedConfig.getExposedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {HttpMethods.GET.name(), HttpMethods.PUT.name()},
                combinedConfig.getAllowedMethods().toArray());
        Assertions.assertArrayEquals(
                new String[] {"http://*.domain1.com", "http://*.domain2.com"},
                combinedConfig.getAllowedOriginPatterns().toArray());
    }

    @Test
    void combine() {
        // Create a config with some values
        CorsMeta priority = new CorsMeta();
        priority.addAllowedOrigin("https://domain1.com");
        priority.addAllowedOriginPattern("http://*.domain1.com");
        priority.addAllowedHeader("header1");
        priority.addExposedHeader("header3");
        priority.addAllowedMethod(HttpMethods.GET.name());
        priority.setMaxAge(123L);
        priority.setAllowCredentials(true);
        priority.setAllowPrivateNetwork(true);

        // Create another config with some different values
        CorsMeta other = new CorsMeta();
        other.addAllowedOrigin("https://domain2.com");
        other.addAllowedOriginPattern("http://*.domain2.com");
        other.addAllowedHeader("header2");
        other.addExposedHeader("header4");
        other.addAllowedMethod(HttpMethods.PUT.name());
        other.setMaxAge(456L);
        other.setAllowCredentials(false);
        other.setAllowPrivateNetwork(false);

        // Combine the configs
        priority = CorsMeta.combine(priority, other);

        // Assert the combined config
        Assertions.assertNotNull(priority);
        Assertions.assertArrayEquals(
                new String[] {"https://domain1.com", "https://domain2.com"},
                priority.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"header1", "header2"},
                priority.getAllowedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {"header3", "header4"},
                priority.getExposedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {HttpMethods.GET.name(), HttpMethods.PUT.name()},
                priority.getAllowedMethods().toArray());
        Assertions.assertEquals(Long.valueOf(123L), priority.getMaxAge());
        Assertions.assertTrue(priority.getAllowCredentials());
        Assertions.assertTrue(priority.getAllowPrivateNetwork());
        Assertions.assertArrayEquals(
                new String[] {"http://*.domain1.com", "http://*.domain2.com"},
                priority.getAllowedOriginPatterns().toArray());
    }

    @Test
    void checkOriginAllowed() {
        // "*" matches
        CorsMeta config = new CorsMeta();
        config.addAllowedOrigin("*");
        Assertions.assertEquals("*", config.checkOrigin("https://domain.com"));

        // "*" does not match together with allowCredentials
        config.setAllowCredentials(true);
        Assertions.assertNull(config.checkOrigin("https://domain.com"));

        // specific origin matches Origin header with or without trailing "/"
        config.setAllowedOrigins(Collections.singletonList("https://domain.com"));
        Assertions.assertEquals("https://domain.com", config.checkOrigin("https://domain.com"));
        Assertions.assertEquals("https://domain.com/", config.checkOrigin("https://domain.com/"));

        // specific origin with trailing "/" matches Origin header with or without trailing "/"
        config.setAllowedOrigins(Collections.singletonList("https://domain.com/"));
        Assertions.assertEquals("https://domain.com", config.checkOrigin("https://domain.com"));
        Assertions.assertEquals("https://domain.com/", config.checkOrigin("https://domain.com/"));

        config.setAllowCredentials(false);
        Assertions.assertEquals("https://domain.com", config.checkOrigin("https://domain.com"));
    }

    @Test
    void checkOriginNotAllowed() {
        CorsMeta config = new CorsMeta();
        Assertions.assertNull(config.checkOrigin(null));
        Assertions.assertNull(config.checkOrigin("https://domain.com"));

        config.addAllowedOrigin("*");
        Assertions.assertNull(config.checkOrigin(null));

        config.setAllowedOrigins(Collections.singletonList("https://domain1.com"));
        Assertions.assertNull(config.checkOrigin("https://domain2.com"));

        config.setAllowedOrigins(new ArrayList<>());
        Assertions.assertNull(config.checkOrigin("https://domain.com"));
    }

    @Test
    void checkOriginPatternAllowed() {
        CorsMeta config = new CorsMeta();
        Assertions.assertNull(config.checkOrigin("https://domain.com"));

        config.applyPermitDefaultValues();
        Assertions.assertEquals("*", config.checkOrigin("https://domain.com"));

        config.setAllowCredentials(true);
        Assertions.assertNull(config.checkOrigin("https://domain.com"));
        config.addAllowedOriginPattern("https://*.domain.com");
        Assertions.assertEquals("https://example.domain.com", config.checkOrigin("https://example.domain.com"));

        config.addAllowedOriginPattern("https://*.port.domain.com:[*]");
        Assertions.assertEquals(
                "https://example.port.domain.com", config.checkOrigin("https://example.port.domain.com"));
        Assertions.assertEquals(
                "https://example.port.domain.com:1234", config.checkOrigin("https://example.port.domain.com:1234"));

        config.addAllowedOriginPattern("https://*.specific.port.com:[8080,8081]");
        Assertions.assertEquals(
                "https://example.specific.port.com:8080", config.checkOrigin("https://example.specific.port.com:8080"));
        Assertions.assertEquals(
                "https://example.specific.port.com:8081", config.checkOrigin("https://example.specific.port.com:8081"));
        Assertions.assertNull(config.checkOrigin("https://example.specific.port.com:1234"));

        config.setAllowCredentials(false);
        Assertions.assertEquals("https://example.domain.com", config.checkOrigin("https://example.domain.com"));
    }

    @Test
    void checkOriginPatternNotAllowed() {
        CorsMeta config = new CorsMeta();
        Assertions.assertNull(config.checkOrigin(null));
        Assertions.assertNull(config.checkOrigin("https://domain.com"));

        config.addAllowedOriginPattern("*");
        Assertions.assertNull(config.checkOrigin(null));

        config.setAllowedOriginPatterns(Collections.singletonList("http://*.domain1.com"));
        Assertions.assertNull(config.checkOrigin("https://domain2.com"));

        config.setAllowedOriginPatterns(new ArrayList<>());
        Assertions.assertNull(config.checkOrigin("https://domain.com"));

        config.setAllowedOriginPatterns(Collections.singletonList("https://*.specific.port.com:[8080,8081]"));
        Assertions.assertNull(config.checkOrigin("https://example.specific.port.com:1234"));
    }

    @Test
    void checkMethodAllowed() {
        CorsMeta config = new CorsMeta();
        Assertions.assertArrayEquals(
                new String[] {HttpMethods.GET.name(), HttpMethods.HEAD.name()},
                config.checkHttpMethods(HttpMethods.GET).stream()
                        .map(HttpMethods::name)
                        .toArray(String[]::new));

        config.addAllowedMethod("GET");
        Assertions.assertArrayEquals(
                new String[] {HttpMethods.GET.name()},
                config.checkHttpMethods(HttpMethods.GET).stream()
                        .map(HttpMethods::name)
                        .toArray(String[]::new));

        config.addAllowedMethod("POST");
        Assertions.assertArrayEquals(
                new String[] {HttpMethods.GET.name(), HttpMethods.POST.name()},
                config.checkHttpMethods(HttpMethods.GET).stream()
                        .map(HttpMethods::name)
                        .toArray(String[]::new));
        Assertions.assertArrayEquals(
                new String[] {HttpMethods.GET.name(), HttpMethods.POST.name()},
                config.checkHttpMethods(HttpMethods.POST).stream()
                        .map(HttpMethods::name)
                        .toArray(String[]::new));
    }

    @Test
    void checkMethodNotAllowed() {
        CorsMeta config = new CorsMeta();
        Assertions.assertNull(config.checkHttpMethods(null));
        Assertions.assertNull(config.checkHttpMethods(HttpMethods.DELETE));

        config.setAllowedMethods(new ArrayList<>());
        Assertions.assertNull(config.checkHttpMethods(HttpMethods.POST));
    }

    @Test
    void checkHeadersAllowed() {
        CorsMeta config = new CorsMeta();
        Assertions.assertEquals(Collections.emptyList(), config.checkHeaders(Collections.emptyList()));

        config.addAllowedHeader("header1");
        config.addAllowedHeader("header2");

        Assertions.assertArrayEquals(
                new String[] {"header1"},
                config.checkHeaders(Collections.singletonList("header1")).toArray());
        Assertions.assertArrayEquals(
                new String[] {"header1", "header2"},
                config.checkHeaders(Arrays.asList("header1", "header2")).toArray());
        Assertions.assertArrayEquals(
                new String[] {"header1", "header2"},
                config.checkHeaders(Arrays.asList("header1", "header2", "header3"))
                        .toArray());
    }

    @Test
    void checkHeadersNotAllowed() {
        CorsMeta config = new CorsMeta();
        Assertions.assertNull(config.checkHeaders(null));
        Assertions.assertNull(config.checkHeaders(Collections.singletonList("header1")));

        config.setAllowedHeaders(Collections.emptyList());
        Assertions.assertNull(config.checkHeaders(Collections.singletonList("header1")));

        config.addAllowedHeader("header2");
        config.addAllowedHeader("header3");
        Assertions.assertNull(config.checkHeaders(Collections.singletonList("header1")));
    }

    @Test
    void changePermitDefaultValues() {
        CorsMeta config = new CorsMeta().applyPermitDefaultValues();
        config.addAllowedOrigin("https://domain.com");
        config.addAllowedHeader("header1");
        config.addAllowedMethod("PATCH");

        Assertions.assertArrayEquals(
                new String[] {"*", "https://domain.com"},
                config.getAllowedOrigins().toArray());
        Assertions.assertArrayEquals(
                new String[] {"*", "header1"}, config.getAllowedHeaders().toArray());
        Assertions.assertArrayEquals(
                new String[] {"GET", "HEAD", "POST", "PATCH"},
                config.getAllowedMethods().toArray());
    }

    @Test
    void permitDefaultDoesntSetOriginWhenPatternPresent() {
        CorsMeta config = new CorsMeta();
        config.addAllowedOriginPattern("http://*.com");
        config = config.applyPermitDefaultValues();

        Assertions.assertNull(config.getAllowedOrigins());
        Assertions.assertArrayEquals(
                new String[] {"http://*.com"}, config.getAllowedOriginPatterns().toArray());
    }
}
