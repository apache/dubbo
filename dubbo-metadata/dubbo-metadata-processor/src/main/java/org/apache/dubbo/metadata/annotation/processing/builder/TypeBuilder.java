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
package org.apache.dubbo.metadata.annotation.processing.builder;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import java.util.Map;

@SPI
public interface TypeBuilder<T extends TypeMirror> extends Prioritized {

    /**
     * Test the specified {@link TypeMirror type} is accepted or not
     *
     * @param processingEnv {@link ProcessingEnvironment}
     * @param type          {@link TypeMirror type}
     * @return <code>true</code> if accepted
     */
    boolean accept(ProcessingEnvironment processingEnv, TypeMirror type);

    /**
     * Build the instance of {@link TypeDefinition}
     *
     * @param processingEnv  {@link ProcessingEnvironment}
     * @param type           {@link T type}
     * @return an instance of {@link TypeDefinition}
     */
    TypeDefinition build(ProcessingEnvironment processingEnv, T type, Map<String, TypeDefinition> typeCache);
}
