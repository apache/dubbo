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
package org.apache.dubbo.metadata.annotation.processing;

import org.apache.dubbo.metadata.annotation.processing.rest.ServiceRestMetadataProcessor;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

import com.google.gson.Gson;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;
import static javax.lang.model.util.ElementFilter.typesIn;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getOverrideMethod;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.getAnnotation;

/**
 * The {@link Processor} class to generate the metadata of REST from the classes that are annotated by Dubbo's
 *
 * @Service
 * @see Processor
 * @since 2.7.5
 */
public class RestMetadataServiceAnnotationProcessor extends AbstractServiceAnnotationProcessor {

    private Iterable<ServiceRestMetadataProcessor> metadataProcessors;

    private Collection<ServiceRestMetadata> serviceRestMetadata;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.metadataProcessors = load(ServiceRestMetadataProcessor.class);
        this.serviceRestMetadata = new LinkedHashSet<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        typesIn(roundEnv.getRootElements()).forEach(serviceType -> process(processingEnv, serviceType, annotations));

        if (roundEnv.processingOver()) {
            serviceRestMetadata.forEach(serviceRestMetadata -> {
                Gson gson = new Gson();
                System.out.println(gson.toJson(serviceRestMetadata));
            });
        }

        return false;
    }

    private void process(ProcessingEnvironment processingEnv, TypeElement serviceType,
                         Set<? extends TypeElement> annotations) {
        stream(metadataProcessors.spliterator(), false)
                .filter(processor -> processor.supports(processingEnv, serviceType))
                .map(processor -> processor.process(processingEnv, serviceType, annotations))
                .forEach(serviceRestMetadata::add);
    }

    private void process(TypeElement annotatedClass) {
        List<? extends AnnotationMirror> annotationMirrors = annotatedClass.getAnnotationMirrors();
        AnnotationMirror annotationMirror = getAnnotation(annotationMirrors);
        TypeElement interfaceClass = resolveServiceInterface(annotatedClass, annotationMirror);
        List<? extends ExecutableElement> interfaceMethods = getActualMethods(interfaceClass);
        processMethods(annotatedClass, interfaceMethods);
    }

    private void processMethods(TypeElement annotatedClass, List<? extends ExecutableElement> interfaceMethods) {
        interfaceMethods.forEach(method -> processMethod(annotatedClass, method));
    }

    private void processMethod(TypeElement annotatedClass, ExecutableElement interfaceMethod) {

        String interfaceClass = interfaceMethod.getEnclosingElement().toString();

        ExecutableElement classMethod = getOverrideMethod(processingEnv, annotatedClass, interfaceMethod);

//        PackageElement packageElement = getPackageElement(annotatedClass);

        String methodName = classMethod.getSimpleName().toString();
        StringBuilder restMetadata = new StringBuilder("/")
                .append(annotatedClass.toString().replace('.', '/'))
                .append("/")
                .append(methodName);

        List<? extends VariableElement> parameters = classMethod.getParameters();

        for (VariableElement parameter : parameters) {
            restMetadata.append("/{").append(parameter.getSimpleName()).append("}/");
        }

    }
}
