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

import org.apache.dubbo.metadata.annotation.processing.util.ExecutableElementComparator;
import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.rest.RequestMetadata;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.rpc.model.ApplicationModel;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
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
import static java.util.Collections.sort;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.dubbo.metadata.annotation.processing.builder.MethodDefinitionBuilder.build;
import static org.apache.dubbo.metadata.annotation.processing.util.LoggerUtils.info;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getOverrideMethod;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getPublicNonStaticMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.getGroup;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.getVersion;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.resolveServiceInterfaceName;

/**
 * Abstract {@link ServiceRestMetadataResolver} implementation
 *
 * @since 2.7.6
 */
public abstract class AbstractServiceRestMetadataResolver implements ServiceRestMetadataResolver {

    private final static ThreadLocal<Map<String, Object>> threadLocalCache = withInitial(HashMap::new);

    private final static Map<String, List<AnnotatedMethodParameterProcessor>> parameterProcessorsMap = loadAnnotatedMethodParameterProcessors();

    private final String processorName = getClass().getSimpleName();

    @Override
    public final ServiceRestMetadata resolve(ProcessingEnvironment processingEnv,
                                             TypeElement serviceType,
                                             Set<? extends TypeElement> annotations) {

        info("%s is processing the service type[%s] with annotations[%s]", processorName, serviceType,
                annotations.stream().map(t -> "@" + t.toString()).collect(Collectors.joining(",")));

        ServiceRestMetadata serviceRestMetadata = new ServiceRestMetadata();

        Elements elements = processingEnv.getElementUtils();

        try {
            AnnotationMirror serviceAnnotation = getAnnotation(serviceType);
            String serviceInterfaceName = resolveServiceInterfaceName(serviceType, serviceAnnotation);
            serviceRestMetadata.setServiceInterface(serviceInterfaceName);
            serviceRestMetadata.setGroup(getGroup(serviceAnnotation));
            serviceRestMetadata.setVersion(getVersion(serviceAnnotation));

            TypeElement serviceInterfaceType = elements.getTypeElement(serviceInterfaceName);

            List<? extends ExecutableElement> serviceMethods = new LinkedList<>(getPublicNonStaticMethods(serviceInterfaceType, Object.class));

            // Sorts
            sort(serviceMethods, ExecutableElementComparator.INSTANCE);

            serviceMethods.forEach(serviceMethod -> {
                resolveRestMethodMetadata(processingEnv, serviceType, serviceInterfaceType, serviceMethod, serviceRestMetadata)
                    .ifPresent(serviceRestMetadata.getMeta()::add);
            });

        } finally {
            clearCache();
        }

        info("The %s's process result : %s", processorName, serviceRestMetadata);

        return serviceRestMetadata;
    }

    protected Optional<RestMethodMetadata> resolveRestMethodMetadata(ProcessingEnvironment processingEnv,
                                                                     TypeElement serviceType,
                                                                     TypeElement serviceInterfaceType,
                                                                     ExecutableElement serviceMethod,
                                                                     ServiceRestMetadata serviceRestMetadata) {

        ExecutableElement restCapableMethod = findRestCapableMethod(processingEnv, serviceType, serviceInterfaceType, serviceMethod);

        if (restCapableMethod == null) { // if can't be found
            return empty();
        }

        String requestPath = resolveRequestPath(processingEnv, serviceType, restCapableMethod); // requestPath is required

        if (requestPath == null) {
            return empty();
        }

        String requestMethod = resolveRequestMethod(processingEnv, serviceType, restCapableMethod); // requestMethod is required

        if (requestMethod == null) {
            return empty();
        }

        RestMethodMetadata metadata = new RestMethodMetadata();

        MethodDefinition methodDefinition = resolveMethodDefinition(processingEnv, serviceType, restCapableMethod);
        // Set MethodDefinition
        metadata.setMethod(methodDefinition);

        // process the annotated method parameters
        processAnnotatedMethodParameters(restCapableMethod, serviceType, metadata);

        // process produces
        Set<String> produces = new LinkedHashSet<>();
        processProduces(processingEnv, serviceType, restCapableMethod, produces);

        // process consumes
        Set<String> consumes = new LinkedHashSet<>();
        processConsumes(processingEnv, serviceType, restCapableMethod, consumes);

        // Initialize RequestMetadata
        RequestMetadata request = metadata.getRequest();
        request.setPath(requestPath);
        request.appendContextPathFromUrl(serviceRestMetadata.getContextPathFromUrl());

        request.setMethod(requestMethod);
        request.setProduces(produces);
        request.setConsumes(consumes);

        // Post-Process
        postProcessRestMethodMetadata(processingEnv, serviceType, serviceMethod, metadata);

        return of(metadata);
    }

    /**
     * Find the method with the capable for REST from the specified service method and its override method
     *
     * @param processingEnv        {@link ProcessingEnvironment}
     * @param serviceType
     * @param serviceInterfaceType
     * @param serviceMethod
     * @return <code>null</code> if can't be found
     */
    private ExecutableElement findRestCapableMethod(ProcessingEnvironment processingEnv,
                                                    TypeElement serviceType,
                                                    TypeElement serviceInterfaceType,
                                                    ExecutableElement serviceMethod) {
        // try to judge the override first
        ExecutableElement overrideMethod = getOverrideMethod(processingEnv, serviceType, serviceMethod);
        if (supports(processingEnv, serviceType, serviceInterfaceType, overrideMethod)) {
            return overrideMethod;
        }
        // or, try to judge the declared method
        return supports(processingEnv, serviceType, serviceInterfaceType, serviceMethod) ? serviceMethod : null;
    }

    /**
     * Does the specified method support REST or not ?
     *
     * @param processingEnv {@link ProcessingEnvironment}
     * @param method        the method may be declared on the interface or class
     * @return if supports, return <code>true</code>, or <code>false</code>
     */
    protected abstract boolean supports(ProcessingEnvironment processingEnv,
                                        TypeElement serviceType,
                                        TypeElement serviceInterfaceType,
                                        ExecutableElement method);


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

    protected abstract String resolveRequestPath(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                                 ExecutableElement method);

    protected abstract String resolveRequestMethod(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                                   ExecutableElement method);

    protected MethodDefinition resolveMethodDefinition(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                                       ExecutableElement method) {
        return build(processingEnv, method, new HashMap<>());
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

        ApplicationModel.defaultModel()
                .getExtensionLoader(AnnotatedMethodParameterProcessor.class)
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
