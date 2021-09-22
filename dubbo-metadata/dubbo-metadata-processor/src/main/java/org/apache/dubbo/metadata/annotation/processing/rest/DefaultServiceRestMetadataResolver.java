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
package org.apache.dubbo.metadata.annotation.processing.rest;

import org.apache.dubbo.common.convert.Converter;
import org.apache.dubbo.metadata.annotation.processing.rest.jaxrs.JAXRSServiceRestMetadataResolver;
import org.apache.dubbo.metadata.annotation.processing.rest.springmvc.SpringMvcServiceRestMetadataResolver;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.apache.dubbo.common.convert.Converter.getConverter;
import static org.apache.dubbo.common.utils.ClassUtils.forName;
import static org.apache.dubbo.common.utils.StringUtils.SLASH_CHAR;
import static org.apache.dubbo.metadata.annotation.processing.util.LoggerUtils.warn;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.resolveServiceInterfaceName;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.findInterface;

/**
 * The default implementation of {@link ServiceRestMetadataResolver}
 *
 * @since 2.7.6
 */
public class DefaultServiceRestMetadataResolver extends AbstractServiceRestMetadataResolver {

    private static final char PACKAGE_SEPARATOR = '.';

    private static final char PATH_SEPARATOR = SLASH_CHAR;

    private static final String HTTP_REQUEST_METHOD = "POST";

    private static final List<String> MEDIA_TYPES = asList(
            "application/json;",
            "application/*+json",
            "application/xml;charset=UTF-8",
            "text/xml;charset=UTF-8",
            "application/*+xml;charset=UTF-8"
    );

    private final Set<ExecutableElement> hasComplexParameterTypeMethods = new HashSet<>();

    @Override
    public boolean supports(ProcessingEnvironment processingEnvironment, TypeElement serviceType) {
        return !JAXRSServiceRestMetadataResolver.supports(serviceType) &&
                !SpringMvcServiceRestMetadataResolver.supports(serviceType);
    }

    @Override
    protected boolean supports(ProcessingEnvironment processingEnv, TypeElement serviceType,
                               TypeElement serviceInterfaceType, ExecutableElement method) {
        // TODO add some criterion
        return true;
    }

    @Override
    protected String resolveRequestPath(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                        ExecutableElement method) {

        AnnotationMirror serviceAnnotation = getAnnotation(serviceType);

        String serviceInterfaceName = resolveServiceInterfaceName(serviceType, serviceAnnotation);

        TypeMirror serviceInterface = findInterface(serviceType.asType(), serviceInterfaceName);

        StringBuilder requestPathBuilder = new StringBuilder();
        // the name of service type as the root path
        String rootPath = buildRootPath(serviceInterface);
        // the method name as the sub path
        String subPath = buildSubPath(method);

        requestPathBuilder.append(rootPath).append(subPath);
        // the methods' parameters as the the path variables
        List<? extends VariableElement> parameters = method.getParameters();

        for (int i = 0; i < parameters.size(); i++) {
            VariableElement parameter = parameters.get(i);
            TypeMirror parameterType = parameter.asType();
            if (isComplexType(parameterType)) {
                if (addComplexParameterType(method)) {
                    continue;
                } else {
                    // The count of complex types must be only one, or return immediately
                    warn("The method[%s] contains more than one complex parameter type, " +
                            "thus it will not be chosen as the REST service", method.toString());
                }
            }
            String parameterName = parameter.getSimpleName().toString();
            // If "-parameters" option is enabled, take the parameter name as the path variable name,
            // or use the index of parameter
            String pathVariableName = isEnabledParametersCompilerOption(parameterName) ? parameterName : valueOf(i);
            requestPathBuilder.append(PATH_SEPARATOR).append('{').append(pathVariableName).append('}');
        }

        return requestPathBuilder.toString();
    }

    private String buildRootPath(TypeMirror serviceInterface) {
        return PATH_SEPARATOR + serviceInterface.toString().replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
    }

    private String buildSubPath(ExecutableElement method) {
        return PATH_SEPARATOR + method.getSimpleName().toString();
    }

    private boolean isEnabledParametersCompilerOption(String parameterName) {
        return !parameterName.startsWith("arg");
    }

    private boolean isComplexType(TypeMirror parameterType) {
        return !supportsPathVariableType(parameterType);
    }

    /**
     * Supports the type of parameter or not, based by {@link Converter}'s conversion feature
     *
     * @param parameterType the type of parameter
     * @return if supports, this method will return <code>true</code>, or <code>false</code>
     */
    private boolean supportsPathVariableType(TypeMirror parameterType) {
        String className = parameterType.toString();
        ClassLoader classLoader = getClass().getClassLoader();
        boolean supported;
        try {
            Class<?> targetType = forName(className, classLoader);
            supported = getConverter(String.class, targetType) != null;
        } catch (ClassNotFoundException e) {
            supported = false;
        }
        return supported;
    }

    @Override
    protected String resolveRequestMethod(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                          ExecutableElement method) {
        return HTTP_REQUEST_METHOD;
    }

    @Override
    protected void processProduces(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                   ExecutableElement method, Set<String> produces) {
        TypeMirror returnType = method.getReturnType();
        if (isComplexType(returnType)) {
            produces.addAll(MEDIA_TYPES);
        }
    }

    @Override
    protected void processConsumes(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                   ExecutableElement method, Set<String> consumes) {
        if (hasComplexParameterType(method)) {
            consumes.addAll(MEDIA_TYPES);
        }
    }

    private boolean addComplexParameterType(ExecutableElement method) {
        return hasComplexParameterTypeMethods.add(method);
    }

    private boolean hasComplexParameterType(ExecutableElement method) {
        return hasComplexParameterTypeMethods.remove(method);
    }
}
