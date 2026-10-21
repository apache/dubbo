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
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtension;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtensionAdapter;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingResolver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class RestSpring6ApiParityTest {

    private static final String PACKAGE_NAME = "org.apache.dubbo.rpc.protocol.tri.rest.support.spring.";

    private static final List<String> API_CLASS_NAMES = List.of(
            "AbstractSpringArgumentResolver",
            "Annotations",
            "BeanArgumentBinder",
            "BindParamArgumentResolver",
            "ConfigurationWrapper",
            "CookieValueArgumentResolver",
            "FallbackArgumentResolver",
            "HandlerInterceptorAdapter",
            "Helper",
            "MatrixVariableArgumentResolver",
            "ModelAttributeArgumentResolver",
            "MultiValueMapCreator",
            "PathVariableArgumentResolver",
            "RequestAttributeArgumentResolver",
            "RequestBodyArgumentResolver",
            "RequestHeaderArgumentResolver",
            "RequestParamArgumentResolver",
            "RequestPartArgumentResolver",
            "RestSpringScopeModelInitializer",
            "SpringMiscArgumentResolver",
            "SpringMvcRequestMappingResolver",
            "SpringResponseRestFilter",
            "SpringRestToolKit");

    @Test
    void exposesExactlyTheGoldenPublicApiAndActivationContract() throws Exception {
        assertEquals(readGolden(), apiGolden());
    }

    @Test
    void keepsAllTwentyThreeContractClassesTopLevel() throws ClassNotFoundException {
        assertEquals(23, API_CLASS_NAMES.size());
        assertEquals(23, API_CLASS_NAMES.stream().distinct().count());
        for (String simpleName : API_CLASS_NAMES) {
            Class<?> type = loadClass(simpleName);
            assertNull(type.getEnclosingClass(), type.getName());
            assertFalse(type.isSynthetic(), type.getName());
        }
    }

    @Test
    void activatesAllSixteenSpringExtensionsOnTheJakartaClasspath() {
        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            assertEquals(
                    12,
                    springActivationCount(frameworkModel
                            .getExtensionLoader(ArgumentResolver.class)
                            .getActivateExtensions()));
            assertEquals(
                    1,
                    springActivationCount(frameworkModel
                            .getExtensionLoader(ArgumentConverter.class)
                            .getActivateExtensions()));
            assertEquals(
                    1,
                    springActivationCount(frameworkModel
                            .getExtensionLoader(RestExtension.class)
                            .getActivateExtensions()));
            assertEquals(
                    1,
                    springActivationCount(frameworkModel
                            .getExtensionLoader(RestExtensionAdapter.class)
                            .getActivateExtensions()));
            assertEquals(
                    1,
                    springActivationCount(frameworkModel
                            .getExtensionLoader(RequestMappingResolver.class)
                            .getActivateExtensions()));
        } finally {
            frameworkModel.destroy();
        }
    }

    private static long springActivationCount(List<?> extensions) {
        return extensions.stream()
                .filter(extension -> extension
                        .getClass()
                        .getPackageName()
                        .equals(PACKAGE_NAME.substring(0, PACKAGE_NAME.length() - 1)))
                .count();
    }

    private static String apiPayload() throws ClassNotFoundException {
        List<String> entries = new ArrayList<>();
        for (String simpleName : API_CLASS_NAMES) {
            Class<?> type = loadClass(simpleName);
            entries.add(classEntry(type));
            addActivationEntry(entries, type);
            Arrays.stream(type.getDeclaredFields())
                    .filter(field -> isApi(field.getModifiers()))
                    .map(RestSpring6ApiParityTest::fieldEntry)
                    .forEach(entries::add);
            Arrays.stream(type.getDeclaredConstructors())
                    .filter(constructor -> isApi(constructor.getModifiers()))
                    .map(RestSpring6ApiParityTest::constructorEntry)
                    .forEach(entries::add);
            Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> isApi(method.getModifiers()))
                    .map(RestSpring6ApiParityTest::methodEntry)
                    .forEach(entries::add);
        }
        Collections.sort(entries);
        return String.join("\n", entries) + "\n";
    }

    private static String apiGolden() throws ClassNotFoundException, NoSuchAlgorithmException {
        String payload = apiPayload();
        String digest = HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        return "format=rest-spring6-public-api-v1\nclasses="
                + API_CLASS_NAMES.size()
                + "\nentries="
                + payload.lines().count()
                + "\nsha256="
                + digest
                + "\n";
    }

    private static Class<?> loadClass(String simpleName) throws ClassNotFoundException {
        return Class.forName(PACKAGE_NAME + simpleName, false, RestSpring6ApiParityTest.class.getClassLoader());
    }

    private static boolean isApi(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private static String classEntry(Class<?> type) {
        Type superType = type.getGenericSuperclass();
        String interfaces = Arrays.stream(type.getGenericInterfaces())
                .map(Type::getTypeName)
                .sorted()
                .collect(Collectors.joining(","));
        String typeParameters = Arrays.stream(type.getTypeParameters())
                .map(RestSpring6ApiParityTest::typeParameter)
                .collect(Collectors.joining(","));
        return "class "
                + type.getName()
                + " modifiers="
                + Modifier.toString(type.getModifiers() & Modifier.classModifiers())
                + " superclass="
                + (superType == null ? "" : superType.getTypeName())
                + " interfaces="
                + interfaces
                + " typeParameters="
                + typeParameters;
    }

    private static String typeParameter(TypeVariable<?> variable) {
        return variable.getName() + ":"
                + Arrays.stream(variable.getBounds()).map(Type::getTypeName).collect(Collectors.joining("&"));
    }

    private static void addActivationEntry(List<String> entries, Class<?> type) {
        Activate activate = type.getAnnotation(Activate.class);
        if (activate == null) {
            return;
        }
        entries.add("activate "
                + type.getName()
                + " group="
                + Arrays.toString(activate.group())
                + " value="
                + Arrays.toString(activate.value())
                + " before="
                + Arrays.toString(activate.before())
                + " after="
                + Arrays.toString(activate.after())
                + " order="
                + activate.order()
                + " onClass="
                + Arrays.toString(activate.onClass()));
    }

    private static String fieldEntry(Field field) {
        return "field "
                + field.getDeclaringClass().getName()
                + "#"
                + field.getName()
                + " "
                + descriptor(field.getType())
                + " modifiers="
                + Modifier.toString(field.getModifiers() & Modifier.fieldModifiers())
                + " generic="
                + field.getGenericType().getTypeName();
    }

    private static String constructorEntry(Constructor<?> constructor) {
        return "constructor "
                + constructor.getDeclaringClass().getName()
                + descriptor(constructor.getParameterTypes(), void.class)
                + " modifiers="
                + Modifier.toString(constructor.getModifiers() & Modifier.constructorModifiers())
                + " genericParameters="
                + typeNames(constructor.getGenericParameterTypes())
                + " throws="
                + typeNames(constructor.getGenericExceptionTypes());
    }

    private static String methodEntry(Method method) {
        return "method "
                + method.getDeclaringClass().getName()
                + "#"
                + method.getName()
                + descriptor(method.getParameterTypes(), method.getReturnType())
                + " modifiers="
                + Modifier.toString(method.getModifiers() & Modifier.methodModifiers())
                + " genericReturn="
                + method.getGenericReturnType().getTypeName()
                + " genericParameters="
                + typeNames(method.getGenericParameterTypes())
                + " throws="
                + typeNames(method.getGenericExceptionTypes())
                + " bridge="
                + method.isBridge()
                + " synthetic="
                + method.isSynthetic();
    }

    private static String typeNames(Type[] types) {
        return Arrays.stream(types).map(Type::getTypeName).collect(Collectors.joining(","));
    }

    private static String descriptor(Class<?>[] parameterTypes, Class<?> returnType) {
        return Arrays.stream(parameterTypes)
                        .map(RestSpring6ApiParityTest::descriptor)
                        .collect(Collectors.joining("", "(", ")"))
                + descriptor(returnType);
    }

    private static String descriptor(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == void.class) {
                return "V";
            }
            if (type == boolean.class) {
                return "Z";
            }
            if (type == byte.class) {
                return "B";
            }
            if (type == char.class) {
                return "C";
            }
            if (type == short.class) {
                return "S";
            }
            if (type == int.class) {
                return "I";
            }
            if (type == long.class) {
                return "J";
            }
            if (type == float.class) {
                return "F";
            }
            return "D";
        }
        if (type.isArray()) {
            return type.getName().replace('.', '/');
        }
        return "L" + type.getName().replace('.', '/') + ";";
    }

    private static String readGolden() throws IOException {
        try (InputStream input = RestSpring6ApiParityTest.class.getResourceAsStream("/rest-spring6-api-golden.txt")) {
            if (input == null) {
                throw new IOException("Missing rest-spring6-api-golden.txt");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }
}
