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

import org.apache.dubbo.metadata.tools.Compiler;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.AbstractAnnotationProcessingTest.testInstanceHolder;

public class CompilerInvocationInterceptor implements InvocationInterceptor {

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {
        Set<Class<?>> classesToBeCompiled = new LinkedHashSet<>();
        AbstractAnnotationProcessingTest abstractAnnotationProcessingTest = testInstanceHolder.get();
        classesToBeCompiled.add(getClass());
        abstractAnnotationProcessingTest.addCompiledClasses(classesToBeCompiled);
        Compiler compiler = new Compiler();
        compiler.processors(new AnnotationProcessingTestProcessor(abstractAnnotationProcessingTest, invocation, invocationContext, extensionContext));
        compiler.compile(classesToBeCompiled.toArray(new Class[0]));
    }
}
