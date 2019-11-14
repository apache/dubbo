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

import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.rest.RequestMetadata;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.ThreadLocal.withInitial;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.metadata.annotation.processing.builder.MethodDefinitionBuilder.build;
import static org.apache.dubbo.metadata.annotation.processing.util.LoggerUtils.info;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getPublicNonStaticMethods;
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

    private final static ThreadLocal<Map<String, Object>> threadLocalCache = withInitial(HashMap::new);

    private final static Map<String, List<AnnotatedMethodParameterProcessor>> parameterProcessorsMap = loadAnnotatedMethodParameterProcessors();

    private final String processorName = getClass().getSimpleName();

    @Override
    public final ServiceRestMetadata process(ProcessingEnvironment processingEnv,
                                             TypeElement serviceType,
                                             Set<? extends TypeElement> annotations) {

        info("%s is processing the service type[%s] with annotations[%s]", processorName, serviceType,
                annotations.stream().map(t -> "@" + t.toString()).collect(Collectors.joining(",")));

        ServiceRestMetadata serviceRestMetadata = new ServiceRestMetadata();

        try {
            AnnotationMirror serviceAnnotation = getAnnotation(serviceType);
            serviceRestMetadata.setServiceInterface(resolveServiceInterfaceName(serviceType, serviceAnnotation));
            serviceRestMetadata.setGroup(getGroup(serviceAnnotation));
            serviceRestMetadata.setVersion(getVersion(serviceAnnotation));

            List<? extends ExecutableElement> methods = getPublicNonStaticMethods(serviceType, Object.class);

            methods.forEach(method -> {
                processRestMethodMetadata(processingEnv, serviceType, method)
                        .ifPresent(serviceRestMetadata.getMeta()::add);
            });
        } finally {
            clearCache();
        }

        info("The %s's process result : %s", processorName, serviceRestMetadata);

        return serviceRestMetadata;
    }

    protected Optional<RestMethodMetadata> processRestMethodMetadata(ProcessingEnvironment processingEnv,
                                                                     TypeElement serviceType, ExecutableElement method) {

        String requestPath = getRequestPath(processingEnv, serviceType, method); // requestPath is required

        if (requestPath == null) {
            return empty();
        }

        String requestMethod = getRequestMethod(processingEnv, serviceType, method); // requestMethod is required

        if (requestMethod == null) {
            return empty();
        }

        RestMethodMetadata metadata = new RestMethodMetadata();

        MethodDefinition methodDefinition = getMethodDefinition(processingEnv, serviceType, method);
        // Set MethodDefinition
        metadata.setMethod(methodDefinition);

        // process the annotated method parameters
        processAnnotatedMethodParameters(method, serviceType, metadata);

        // process produces
        Set<String> produces = new LinkedHashSet<>();
        processProduces(processingEnv, serviceType, method, produces);

        // process consumes
        Set<String> consumes = new LinkedHashSet<>();
        processConsumes(processingEnv, serviceType, method, consumes);

        // Initialize RequestMetadata
        RequestMetadata request = metadata.getRequest();
        request.setPath(requestPath);
        request.setMethod(requestMethod);
        request.setProduces(produces);
        request.setConsumes(consumes);

        // Post-Process
        postProcessRestMethodMetadata(processingEnv, serviceType, method, metadata);

        return of(metadata);
    }

    /**
     * Post-Process for {@link RestMethodMetadata}, sub-type could override this method for further works
     *
     * @param processingEnv {@link ProcessingEnvironment}
     * @param serviceType   The type that @Service annotated
     * @param method        The public method of <code>serviceType</code>
     * @param metadata      {@link RestMethodMetadata} maybe updated
     */
    protected void postProcessRestMethodMetadata(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                                 ExecutableElement method, RestMethodMetadata metadata) {
    }

    protected abstract String getRequestPath(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                             ExecutableElement method);

    protected abstract String getRequestMethod(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                               ExecutableElement method);

    protected MethodDefinition getMethodDefinition(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                                   ExecutableElement method) {
        return build(processingEnv, method);
    }

    protected void processAnnotatedMethodParameters(ExecutableElement method, TypeElement type,
                                                    RestMethodMetadata metadata) {
        List<? extends VariableElement> methodParameters = method.getParameters();
        int size = methodParameters.size();
        for (int i = 0; i < size; i++) {
            VariableElement parameter = methodParameters.get(i);
            // Add indexed parameter name
            metadata.addIndexToName(i, parameter.getSimpleName().toString());
            processAnnotatedMethodParameter(parameter, i, method, type, metadata);
        }
    }

    protected void processAnnotatedMethodParameter(VariableElement parameter, int parameterIndex,
                                                   ExecutableElement method, TypeElement serviceType,
                                                   RestMethodMetadata metadata) {

        parameter.getAnnotationMirrors().forEach(annotation -> {
            String annotationType = annotation.getAnnotationType().toString();
            parameterProcessorsMap.getOrDefault(annotationType, emptyList())
                    .forEach(parameterProcessor -> {
                        parameterProcessor.process(annotation, parameter, parameterIndex, method, metadata);
                    });
        });
    }

    protected abstract void processProduces(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                            ExecutableElement method, Set<String> produces);

    protected abstract void processConsumes(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                            ExecutableElement method, Set<String> consumes);

    protected static final void put(String name, Object value) {
        Map<String, Object> cache = getCache();
        cache.put(name, value);
    }

    protected static final <T> T get(String name) throws ClassCastException {
        Map<String, Object> cache = getCache();
        return (T) cache.get(name);
    }

    protected static final <V> V computeIfAbsent(String name, Function<? super String, ? extends V> mappingFunction) {
        return (V) getCache().computeIfAbsent(name, mappingFunction);
    }

    private static Map<String, List<AnnotatedMethodParameterProcessor>> loadAnnotatedMethodParameterProcessors() {
        Map<String, List<AnnotatedMethodParameterProcessor>> parameterProcessorsMap = new LinkedHashMap<>();

//        load(AnnotatedMethodParameterProcessor.class, AnnotatedMethodParameterProcessor.class.getClassLoader())
        getExtensionLoader(AnnotatedMethodParameterProcessor.class)
                .getSupportedExtensionInstances()
                .forEach(processor -> {
                    List<AnnotatedMethodParameterProcessor> processors =
                            parameterProcessorsMap.computeIfAbsent(processor.getAnnotationType(), k -> new LinkedList<>());
                    processors.add(processor);
                });

        return parameterProcessorsMap;
    }

    private static Map<String, Object> getCache() {
        return threadLocalCache.get();
    }

    private static void clearCache() {
        Map<String, Object> cache = getCache();
        cache.clear();
        threadLocalCache.remove();
    }
}
