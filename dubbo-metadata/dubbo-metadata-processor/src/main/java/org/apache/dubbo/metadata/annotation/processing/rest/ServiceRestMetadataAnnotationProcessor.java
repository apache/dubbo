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

import org.apache.dubbo.metadata.annotation.processing.AbstractServiceAnnotationProcessor;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import static javax.lang.model.util.ElementFilter.typesIn;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.isServiceAnnotationPresent;

/**
 * The {@link Processor} class to generate the metadata of REST from the classes that are annotated by Dubbo's
 *
 * @Service
 * @see Processor
 * @since 2.7.6
 */
public class ServiceRestMetadataAnnotationProcessor extends AbstractServiceAnnotationProcessor {

    private Set<ServiceRestMetadataResolver> metadataProcessors;

    private ServiceRestMetadataStorage serviceRestMetadataWriter;

    private Set<ServiceRestMetadata> serviceRestMetadata = new LinkedHashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.metadataProcessors = getExtensionLoader(ServiceRestMetadataResolver.class).getSupportedExtensionInstances();
        this.serviceRestMetadataWriter = new ServiceRestMetadataStorage(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        typesIn(roundEnv.getRootElements()).forEach(serviceType -> process(processingEnv, serviceType, annotations));

        if (roundEnv.processingOver()) {
            try {
                serviceRestMetadataWriter.append(serviceRestMetadata);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        return false;
    }

    private void process(ProcessingEnvironment processingEnv, TypeElement serviceType,
                         Set<? extends TypeElement> annotations) {
        metadataProcessors
                .stream()
                .filter(processor -> supports(processor, processingEnv, serviceType))
                .map(processor -> processor.resolve(processingEnv, serviceType, annotations))
                .forEach(serviceRestMetadata::add);
    }

    private boolean supports(ServiceRestMetadataResolver processor, ProcessingEnvironment processingEnv,
                             TypeElement serviceType) {
        //  @Service must be present in service type
        return isServiceAnnotationPresent(serviceType) && processor.supports(processingEnv, serviceType);
    }
}
