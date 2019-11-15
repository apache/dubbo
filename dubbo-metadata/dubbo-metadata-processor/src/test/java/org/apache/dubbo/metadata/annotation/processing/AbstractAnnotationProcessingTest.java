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

import org.apache.dubbo.metadata.annotation.processing.util.TypeUtils;
import org.apache.dubbo.metadata.tools.Compiler;
import org.apache.dubbo.metadata.tools.TestProcessor;

import org.junit.jupiter.api.BeforeEach;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Abstract {@link Annotation} Processing Test case
 *
 * @since 2.7.5
 */
public abstract class AbstractAnnotationProcessingTest {

    protected ProcessingEnvironment processingEnv;

    protected Elements elements;

    protected Types types;

    @BeforeEach
    public final void init() throws IOException {
        Set<Class<?>> classesToBeCompiled = new LinkedHashSet<>();
        classesToBeCompiled.add(getClass());
        addCompiledClasses(classesToBeCompiled);
        TestProcessor testProcessor = new TestProcessor();
        Compiler compiler = new Compiler();
        compiler.processors(testProcessor);
        compiler.compile(classesToBeCompiled.toArray(new Class[0]));
        processingEnv = testProcessor.getProcessingEnvironment();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        beforeEach();
    }

    protected abstract void addCompiledClasses(Set<Class<?>> classesToBeCompiled);

    protected abstract void beforeEach();

    protected TypeElement getType(Class<?> type) {
        return TypeUtils.getType(processingEnv, type);
    }
}
