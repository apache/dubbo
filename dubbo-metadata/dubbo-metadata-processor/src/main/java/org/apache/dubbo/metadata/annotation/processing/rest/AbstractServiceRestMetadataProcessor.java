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

import org.apache.dubbo.metadata.annotation.processing.builder.TypeDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;
import org.apache.dubbo.metadata.rest.MethodRestMetadata;
import org.apache.dubbo.metadata.rest.RequestMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.getGroup;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.getVersion;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.resolveServiceInterfaceName;

/**
 * Abstract {@link ServiceRestMetadataProcessor} implementation
 *
 * @since 2.7.5
 */
public abstract class AbstractServiceRestMetadataProcessor implements ServiceRestMetadataProcessor {

    @Override
    public final ServiceRestMetadata process(ProcessingEnvironment processingEnv,
                                             TypeElement serviceType,
                                             Set<? extends TypeElement> annotations) {

        AnnotationMirror serviceAnnotation = getAnnotation(serviceType);

        ServiceRestMetadata serviceRestMetadata = new ServiceRestMetadata();
        serviceRestMetadata.setServiceInterface(resolveServiceInterfaceName(serviceType, serviceAnnotation));
        serviceRestMetadata.setGroup(getGroup(serviceAnnotation));
        serviceRestMetadata.setVersion(getVersion(serviceAnnotation));
        serviceRestMetadata.setMeta(new HashSet<>());

        List<? extends ExecutableElement> methods = getMethods(processingEnv, serviceType, Object.class);

        methods.forEach(method -> {
            processMethod(processingEnv, serviceType, method)
                    .ifPresent(serviceRestMetadata.getMeta()::add);
        });

        return serviceRestMetadata;
    }

    protected Optional<MethodRestMetadata> processMethod(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method) {
        // Process RequestMetadata (Optional)
        return processRequestMetadata(processingEnv, serviceType, method)
                .map(request -> {
                    MethodRestMetadata metadata = new MethodRestMetadata();
                    metadata.setRequest(request);
                    // Process MethodDefinition (Mandatory)
                    metadata.setMethod(processMethodMetadata(processingEnv, serviceType, method));
                    return metadata;
                });
    }

    protected Optional<RequestMetadata> processRequestMetadata(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                                               ExecutableElement method) {
        String requestPath = resolveRequestPath(processingEnv, serviceType, method); // requestPath is required

        if (requestPath == null) {
            return empty();
        }

        String requestMethod = resolveRequestMethod(processingEnv, serviceType, method);

        if (requestMethod == null) { // requestMethod is required
            return empty();
        }

        // process parameters
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        processRequestParameters(processingEnv, serviceType, method, parameters);

        // process headers
        Map<String, List<String>> headers = new LinkedHashMap<>();
        processRequestHeaders(processingEnv, serviceType, method, headers);

        // process produces
        Set<String> produces = new LinkedHashSet<>();
        processProduces(processingEnv, serviceType, method, produces);

        // process consumes
        Set<String> consumes = new LinkedHashSet<>();
        processConsumes(processingEnv, serviceType, method, consumes);

        // Initialize RequestMetadata
        RequestMetadata request = new RequestMetadata();
        request.setPath(requestPath);
        request.setMethod(requestMethod);
        request.setParams(parameters);
        request.setHeaders(headers);
        request.setProduces(produces);
        request.setConsumes(consumes);

        return of(request);
    }

    protected abstract String resolveRequestPath(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method);

    protected abstract String resolveRequestMethod(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method);

    protected abstract void processRequestParameters(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                                     ExecutableElement method, Map<String, List<String>> parameters);

    protected abstract void processRequestHeaders(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                                  ExecutableElement method, Map<String, List<String>> headers);

    protected abstract void processProduces(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                            ExecutableElement method, Set<String> produces);

    protected abstract void processConsumes(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                            ExecutableElement method, Set<String> consumes);

    protected MethodDefinition processMethodMetadata(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                                     ExecutableElement method) {
        MethodDefinition methodDefinition = new MethodDefinition();
        methodDefinition.setName(getMethodName(method));
        methodDefinition.setReturnType(getReturnType(method));
        methodDefinition.setParameterTypes(getParameterTypes(method));
        methodDefinition.setParameters(getParameters(processingEnv, method));
        return methodDefinition;
    }

    protected String getMethodName(ExecutableElement method) {
        return method.getSimpleName().toString();
    }

    protected String getReturnType(ExecutableElement method) {
        return method.getReturnType().toString();
    }

    protected String[] getParameterTypes(ExecutableElement method) {
        return method.getParameters()
                .stream()
                .map(Element::asType)
                .map(TypeMirror::toString)
                .toArray(String[]::new);
    }

    protected List<TypeDefinition> getParameters(ProcessingEnvironment processingEnv, ExecutableElement method) {
        return method.getParameters().stream()
                .map(element -> TypeDefinitionBuilder.build(processingEnv, element))
                .collect(Collectors.toList());
    }

}
