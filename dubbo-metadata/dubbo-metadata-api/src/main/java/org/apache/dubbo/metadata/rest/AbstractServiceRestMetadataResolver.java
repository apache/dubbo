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
package org.apache.dubbo.metadata.rest;

import org.apache.dubbo.common.utils.MethodComparator;
import org.apache.dubbo.common.utils.ServiceAnnotationResolver;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.metadata.definition.MethodDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.MethodDefinition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableMap;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.common.function.ThrowableFunction.execute;
import static org.apache.dubbo.common.utils.AnnotationUtils.isAnyAnnotationPresent;
import static org.apache.dubbo.common.utils.ClassUtils.forName;
import static org.apache.dubbo.common.utils.ClassUtils.getAllInterfaces;
import static org.apache.dubbo.common.utils.MethodUtils.excludedDeclaredClass;
import static org.apache.dubbo.common.utils.MethodUtils.getAllMethods;
import static org.apache.dubbo.common.utils.MethodUtils.overrides;

/**
 * The abstract {@link ServiceRestMetadataResolver} class to provider some template methods assemble the instance of
 * {@link ServiceRestMetadata} will extended by the sub-classes.
 *
 * @since 2.7.6
 */
public abstract class AbstractServiceRestMetadataResolver implements ServiceRestMetadataResolver {

    private final Map<String, List<AnnotatedMethodParameterProcessor>> parameterProcessorsMap;

    public AbstractServiceRestMetadataResolver() {
        this.parameterProcessorsMap = loadAnnotatedMethodParameterProcessors();
    }

    @Override
    public final boolean supports(Class<?> serviceType) {
        return isImplementedInterface(serviceType) && isServiceAnnotationPresent(serviceType) && supports0(serviceType);
    }

    protected final boolean isImplementedInterface(Class<?> serviceType) {
        return !getAllInterfaces(serviceType).isEmpty();
    }

    protected final boolean isServiceAnnotationPresent(Class<?> serviceType) {
        return isAnyAnnotationPresent(serviceType, DubboService.class, Service.class,
                com.alibaba.dubbo.config.annotation.Service.class);
    }

    /**
     * internal support method
     *
     * @param serviceType Dubbo Service interface or type
     * @return If supports, return <code>true</code>, or <code>false</code>
     */
    protected abstract boolean supports0(Class<?> serviceType);

    @Override
    public final ServiceRestMetadata resolve(Class<?> serviceType) {

        ServiceRestMetadata serviceRestMetadata = new ServiceRestMetadata();

        // Process ServiceRestMetadata
        processServiceRestMetadata(serviceRestMetadata, serviceType);

        // Process RestMethodMetadata
        processAllRestMethodMetadata(serviceRestMetadata, serviceType);

        return serviceRestMetadata;
    }

    /**
     * Process the service type including the sub-routines:
     * <ul>
     *     <li>{@link ServiceRestMetadata#setServiceInterface(String)}</li>
     *     <li>{@link ServiceRestMetadata#setVersion(String)}</li>
     *     <li>{@link ServiceRestMetadata#setGroup(String)}</li>
     * </ul>
     *
     * @param serviceRestMetadata {@link ServiceRestMetadata}
     * @param serviceType         Dubbo Service interface or type
     */
    protected void processServiceRestMetadata(ServiceRestMetadata serviceRestMetadata, Class<?> serviceType) {
        ServiceAnnotationResolver resolver = new ServiceAnnotationResolver(serviceType);
        serviceRestMetadata.setServiceInterface(resolver.resolveInterfaceClassName());
        serviceRestMetadata.setVersion(resolver.resolveVersion());
        serviceRestMetadata.setGroup(resolver.resolveGroup());
    }

    /**
     * Process all {@link RestMethodMetadata}
     *
     * @param serviceRestMetadata {@link ServiceRestMetadata}
     * @param serviceType         Dubbo Service interface or type
     */
    protected void processAllRestMethodMetadata(ServiceRestMetadata serviceRestMetadata, Class<?> serviceType) {
        Class<?> serviceInterfaceClass = resolveServiceInterfaceClass(serviceRestMetadata, serviceType);
        Map<Method, Method> serviceMethodsMap = resolveServiceMethodsMap(serviceType, serviceInterfaceClass);
        for (Map.Entry<Method, Method> entry : serviceMethodsMap.entrySet()) {
            // try the overrider method first
            Method serviceMethod = entry.getKey();
            // If failed, it indicates the overrider method does not contain metadata , then try the declared method
            if (!processRestMethodMetadata(serviceMethod, serviceType, serviceInterfaceClass, serviceRestMetadata.getMeta()::add)) {
                Method declaredServiceMethod = entry.getValue();
                processRestMethodMetadata(declaredServiceMethod, serviceType, serviceInterfaceClass,
                        serviceRestMetadata.getMeta()::add);
            }
        }
    }

    /**
     * Resolve a map of all public services methods from the specified service type and its interface class, whose key is the
     * declared method, and the value is the overrider method
     *
     * @param serviceType           the service interface implementation class
     * @param serviceInterfaceClass the service interface class
     * @return non-null read-only {@link Map}
     */
    protected Map<Method, Method> resolveServiceMethodsMap(Class<?> serviceType, Class<?> serviceInterfaceClass) {
        Map<Method, Method> serviceMethodsMap = new LinkedHashMap<>();
        // exclude the public methods declared in java.lang.Object.class
        List<Method> declaredServiceMethods = new ArrayList<>(getAllMethods(serviceInterfaceClass, excludedDeclaredClass(Object.class)));
        List<Method> serviceMethods = new ArrayList<>(getAllMethods(serviceType, excludedDeclaredClass(Object.class)));

        // sort methods
        sort(declaredServiceMethods, MethodComparator.INSTANCE);
        sort(serviceMethods, MethodComparator.INSTANCE);

        for (Method declaredServiceMethod : declaredServiceMethods) {
            for (Method serviceMethod : serviceMethods) {
                if (overrides(serviceMethod, declaredServiceMethod)) {
                    serviceMethodsMap.put(serviceMethod, declaredServiceMethod);
                    continue;
                }
            }
        }
        // make them to be read-only
        return unmodifiableMap(serviceMethodsMap);
    }

    /**
     * Resolve the class of Dubbo Service interface
     *
     * @param serviceRestMetadata {@link ServiceRestMetadata}
     * @param serviceType         Dubbo Service interface or type
     * @return non-null
     * @throws RuntimeException If the class is not found, the {@link RuntimeException} wraps the cause will be thrown
     */
    protected Class<?> resolveServiceInterfaceClass(ServiceRestMetadata serviceRestMetadata, Class<?> serviceType) {
        return execute(serviceType.getClassLoader(), classLoader -> {
            String serviceInterface = serviceRestMetadata.getServiceInterface();
            return forName(serviceInterface, classLoader);
        });
    }

    /**
     * Process the single {@link RestMethodMetadata} by the specified {@link Consumer} if present
     *
     * @param serviceMethod         Dubbo Service method
     * @param serviceType           Dubbo Service interface or type
     * @param serviceInterfaceClass The type of Dubbo Service interface
     * @param metadataToProcess     {@link RestMethodMetadata} to process if present
     * @return if processed successfully, return <code>true</code>, or <code>false</code>
     */
    protected boolean processRestMethodMetadata(Method serviceMethod, Class<?> serviceType,
                                                Class<?> serviceInterfaceClass,
                                                Consumer<RestMethodMetadata> metadataToProcess) {

        if (!isRestCapableMethod(serviceMethod, serviceType, serviceInterfaceClass)) {
            return false;
        }

        String requestPath = resolveRequestPath(serviceMethod, serviceType, serviceInterfaceClass); // requestPath is required

        if (requestPath == null) {
            return false;
        }

        String requestMethod = resolveRequestMethod(serviceMethod, serviceType, serviceInterfaceClass); // requestMethod is required

        if (requestMethod == null) {
            return false;
        }

        RestMethodMetadata metadata = new RestMethodMetadata();

        MethodDefinition methodDefinition = resolveMethodDefinition(serviceMethod, serviceType, serviceInterfaceClass);
        // Set MethodDefinition
        metadata.setMethod(methodDefinition);

        // process the annotated method parameters
        processAnnotatedMethodParameters(serviceMethod, serviceType, serviceInterfaceClass, metadata);

        // process produces
        Set<String> produces = new LinkedHashSet<>();
        processProduces(serviceMethod, serviceType, serviceInterfaceClass, produces);

        // process consumes
        Set<String> consumes = new LinkedHashSet<>();
        processConsumes(serviceMethod, serviceType, serviceInterfaceClass, consumes);

        // Initialize RequestMetadata
        RequestMetadata request = metadata.getRequest();
        request.setPath(requestPath);
        request.setMethod(requestMethod);
        request.setProduces(produces);
        request.setConsumes(consumes);

        // Post-Process
        postResolveRestMethodMetadata(serviceMethod, serviceType, serviceInterfaceClass, metadata);

        // Accept RestMethodMetadata
        metadataToProcess.accept(metadata);

        return true;
    }

    /**
     * Test the service method is capable of REST or not?
     *
     * @param serviceMethod         Dubbo Service method
     * @param serviceType           Dubbo Service interface or type
     * @param serviceInterfaceClass The type of Dubbo Service interface
     * @return If capable, return <code>true</code>
     */
    protected abstract boolean isRestCapableMethod(Method serviceMethod, Class<?> serviceType, Class<?>
            serviceInterfaceClass);

    /**
     * Resolve the request method
     *
     * @param serviceMethod         Dubbo Service method
     * @param serviceType           Dubbo Service interface or type
     * @param serviceInterfaceClass The type of Dubbo Service interface
     * @return if can't be resolve, return <code>null</code>
     */
    protected abstract String resolveRequestMethod(Method serviceMethod, Class<?> serviceType, Class<?>
            serviceInterfaceClass);

    /**
     * Resolve the request path
     *
     * @param serviceMethod         Dubbo Service method
     * @param serviceType           Dubbo Service interface or type
     * @param serviceInterfaceClass The type of Dubbo Service interface
     * @return if can't be resolve, return <code>null</code>
     */
    protected abstract String resolveRequestPath(Method serviceMethod, Class<?> serviceType, Class<?>
            serviceInterfaceClass);

    /**
     * Resolve the {@link MethodDefinition}
     *
     * @param serviceMethod         Dubbo Service method
     * @param serviceType           Dubbo Service interface or type
     * @param serviceInterfaceClass The type of Dubbo Service interface
     * @return if can't be resolve, return <code>null</code>
     * @see MethodDefinitionBuilder
     */
    protected MethodDefinition resolveMethodDefinition(Method serviceMethod, Class<?> serviceType,
                                                       Class<?> serviceInterfaceClass) {
        MethodDefinitionBuilder builder = new MethodDefinitionBuilder();
        return builder.build(serviceMethod);
    }

    private void processAnnotatedMethodParameters(Method serviceMethod, Class<?> serviceType,
                                                  Class<?> serviceInterfaceClass, RestMethodMetadata metadata) {
        int paramCount = serviceMethod.getParameterCount();
        Parameter[] parameters = serviceMethod.getParameters();
        for (int i = 0; i < paramCount; i++) {
            Parameter parameter = parameters[i];
            // Add indexed parameter name
            metadata.addIndexToName(i, parameter.getName());
            processAnnotatedMethodParameter(parameter, i, serviceMethod, serviceType, serviceInterfaceClass, metadata);
        }
    }

    private void processAnnotatedMethodParameter(Parameter parameter, int parameterIndex, Method serviceMethod,
                                                 Class<?> serviceType, Class<?> serviceInterfaceClass,
                                                 RestMethodMetadata metadata) {
        Annotation[] annotations = parameter.getAnnotations();
        for (Annotation annotation : annotations) {
            String annotationType = annotation.annotationType().getName();
            parameterProcessorsMap.getOrDefault(annotationType, emptyList())
                    .forEach(processor -> {
                        processor.process(annotation, parameter, parameterIndex, serviceMethod, serviceType,
                                serviceInterfaceClass, metadata);
                    });
        }
    }

    protected abstract void processProduces(Method serviceMethod, Class<?> serviceType, Class<?>
            serviceInterfaceClass,
                                            Set<String> produces);

    protected abstract void processConsumes(Method serviceMethod, Class<?> serviceType, Class<?>
            serviceInterfaceClass,
                                            Set<String> consumes);

    protected void postResolveRestMethodMetadata(Method serviceMethod, Class<?> serviceType,
                                                 Class<?> serviceInterfaceClass, RestMethodMetadata metadata) {
    }

    private static Map<String, List<AnnotatedMethodParameterProcessor>> loadAnnotatedMethodParameterProcessors() {
        Map<String, List<AnnotatedMethodParameterProcessor>> parameterProcessorsMap = new LinkedHashMap<>();
        getExtensionLoader(AnnotatedMethodParameterProcessor.class)
                .getSupportedExtensionInstances()
                .forEach(processor -> {
                    List<AnnotatedMethodParameterProcessor> processors =
                            parameterProcessorsMap.computeIfAbsent(processor.getAnnotationType(), k -> new LinkedList<>());
                    processors.add(processor);
                });
        return parameterProcessorsMap;
    }
}
