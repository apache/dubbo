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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link ServiceRestMetadataProcessor}
 *
 * @since 2.7.5
 */
public class SpringMvcServiceRestMetadataProcessor extends AbstractServiceRestMetadataProcessor {

    @Override
    protected String resolveRequestPath(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                        ExecutableElement method) {
        return null;
    }

    @Override
    protected String resolveRequestMethod(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                          ExecutableElement method) {
        return null;
    }

    @Override
    protected void processRequestParameters(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                            ExecutableElement method, Map<String, List<String>> parameters) {

    }

    @Override
    protected void processRequestHeaders(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                         ExecutableElement method, Map<String, List<String>> headers) {

    }

    @Override
    protected void processProduces(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                   ExecutableElement method, Set<String> produces) {

    }

    @Override
    protected void processConsumes(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                   ExecutableElement method, Set<String> consumes) {

    }
}
