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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring;

import org.apache.dubbo.common.extension.Activate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestSpring6ServletNamespaceTest {

    private static final String JAKARTA_REQUEST = "jakarta/servlet/http/HttpServletRequest";
    private static final String JAKARTA_RESPONSE = "jakarta/servlet/http/HttpServletResponse";
    private static final String JAVAX_SERVLET = "javax/servlet";

    @Test
    void servletSensitiveActivationGatesRequireJakartaServlet() {
        Activate interceptorActivation = HandlerInterceptorAdapter.class.getAnnotation(Activate.class);
        Activate argumentResolverActivation = SpringMiscArgumentResolver.class.getAnnotation(Activate.class);

        assertNotNull(interceptorActivation);
        assertNotNull(argumentResolverActivation);
        assertArrayEquals(
                new String[] {
                    "org.springframework.web.servlet.HandlerInterceptor", "jakarta.servlet.http.HttpServletRequest"
                },
                interceptorActivation.onClass());
        assertArrayEquals(
                new String[] {
                    "org.springframework.web.context.request.WebRequest", "jakarta.servlet.http.HttpServletRequest"
                },
                argumentResolverActivation.onClass());
    }

    @Test
    void servletSensitiveClassesContainOnlyJakartaServletDescriptors() throws IOException {
        Class<?> interceptorFilter = Arrays.stream(HandlerInterceptorAdapter.class.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("HandlerInterceptorRestFilter"))
                .findFirst()
                .orElseThrow();

        byte[] interceptorBytes = classBytes(interceptorFilter);
        byte[] argumentResolverBytes = classBytes(SpringMiscArgumentResolver.class);
        String interceptorClassFile = new String(interceptorBytes, StandardCharsets.ISO_8859_1);
        String argumentResolverClassFile = new String(argumentResolverBytes, StandardCharsets.ISO_8859_1);

        assertTrue(interceptorClassFile.contains(JAKARTA_REQUEST));
        assertTrue(interceptorClassFile.contains(JAKARTA_RESPONSE));
        assertTrue(argumentResolverClassFile.contains(JAKARTA_REQUEST));
        assertFalse(interceptorClassFile.contains(JAVAX_SERVLET));
        assertFalse(argumentResolverClassFile.contains(JAVAX_SERVLET));
        assertEquals(61, majorVersion(interceptorBytes));
        assertEquals(61, majorVersion(argumentResolverBytes));
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        String resourceName = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resourceName)) {
            assertNotNull(input, resourceName);
            return input.readAllBytes();
        }
    }

    private static int majorVersion(byte[] classBytes) {
        return ((classBytes[6] & 0xff) << 8) | (classBytes[7] & 0xff);
    }
}
