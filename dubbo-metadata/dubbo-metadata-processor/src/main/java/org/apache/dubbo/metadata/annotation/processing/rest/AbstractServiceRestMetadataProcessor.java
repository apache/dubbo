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

import org.apache.dubbo.metadata.rest.MethodRestMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        serviceRestMetadata.setGroup(getVersion(serviceAnnotation));
        serviceRestMetadata.setMeta(new HashSet<>());

        List<? extends ExecutableElement> methods = getMethods(processingEnv, serviceType, Object.class);

        methods.forEach(method -> {
            MethodRestMetadata methodRestMetadata = new MethodRestMetadata();
            processMethod(processingEnv, serviceType, method, methodRestMetadata);
            serviceRestMetadata.getMeta().add(methodRestMetadata);
        });

        return serviceRestMetadata;
    }

    protected abstract void processMethod(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method, MethodRestMetadata metadata);
}
