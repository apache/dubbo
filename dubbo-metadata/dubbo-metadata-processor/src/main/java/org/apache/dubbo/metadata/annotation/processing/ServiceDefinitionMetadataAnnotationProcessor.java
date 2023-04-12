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

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static javax.lang.model.util.ElementFilter.typesIn;
import static org.apache.dubbo.metadata.annotation.processing.builder.ServiceDefinitionBuilder.build;

/**
 * The {@link Processor} class to generate the metadata of {@link ServiceDefinition} whose classes are annotated by Dubbo's @Service
 *
 * @see Processor
 * @since 2.7.6
 */
public class ServiceDefinitionMetadataAnnotationProcessor extends AbstractServiceAnnotationProcessor {

    private List<ServiceDefinition> serviceDefinitions = new LinkedList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        typesIn(roundEnv.getRootElements()).forEach(serviceType -> process(processingEnv, serviceType, annotations));

        if (roundEnv.processingOver()) {
            ClassPathMetadataStorage writer = new ClassPathMetadataStorage(processingEnv);
            writer.write(() -> JsonUtils.toJson(serviceDefinitions), "META-INF/dubbo/service-definitions.json");
        }

        return false;
    }

    private void process(ProcessingEnvironment processingEnv, TypeElement serviceType, Set<? extends TypeElement> annotations) {
        serviceDefinitions.add(build(processingEnv, serviceType));
    }
}
