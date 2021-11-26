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

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * The class to resolve {@link ServiceRestMetadata} based on Annotation Processor Tool
 *
 * @since 2.7.6
 */
@SPI("default")
public interface ServiceRestMetadataResolver extends Prioritized {

    /**
     * Supports or not to the specified service type
     *
     * @param processingEnvironment {@link ProcessingEnvironment}
     * @param serviceType           Dubbo service type or interface
     * @return if supports, return <code>true</code>, or <code>false</code>
     */
    boolean supports(ProcessingEnvironment processingEnvironment, TypeElement serviceType);

    /**
     * Resolve the {@link ServiceRestMetadata} from given service type
     *
     * @param processingEnvironment {@link ProcessingEnvironment}
     * @param serviceType           Dubbo service type or interface
     * @param annotations
     * @return non-null
     */
    ServiceRestMetadata resolve(ProcessingEnvironment processingEnvironment,
                                TypeElement serviceType,
                                Set<? extends TypeElement> annotations);
}
