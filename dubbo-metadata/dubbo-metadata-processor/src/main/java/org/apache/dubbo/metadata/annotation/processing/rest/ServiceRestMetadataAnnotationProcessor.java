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
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static javax.lang.model.util.ElementFilter.typesIn;
import static org.apache.dubbo.common.utils.PrioritizedServiceLoader.loadServices;

/**
 * The {@link Processor} class to generate the metadata of REST from the classes that are annotated by Dubbo's
 *
 * @Service
 * @see Processor
 * @since 2.7.5
 */
public class ServiceRestMetadataAnnotationProcessor extends AbstractServiceAnnotationProcessor {

    private List<ServiceRestMetadataProcessor> metadataProcessors;

    private ServiceRestMetadataWriter serviceRestMetadataWriter;

    private Set<ServiceRestMetadata> serviceRestMetadata;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.metadataProcessors = loadServices(ServiceRestMetadataProcessor.class);
        this.serviceRestMetadataWriter = new ServiceRestMetadataWriter(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        typesIn(roundEnv.getRootElements()).forEach(serviceType -> process(processingEnv, serviceType, annotations));

        if (roundEnv.processingOver()) {
            try {
                serviceRestMetadataWriter.write(serviceRestMetadata);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        return false;
    }

    private void process(ProcessingEnvironment processingEnv, TypeElement serviceType,
                         Set<? extends TypeElement> annotations) {
        serviceRestMetadata = metadataProcessors
                .stream()
                .filter(processor -> processor.supports(processingEnv, serviceType))
                .map(processor -> processor.process(processingEnv, serviceType, annotations))
                .collect(toSet());
    }
}
