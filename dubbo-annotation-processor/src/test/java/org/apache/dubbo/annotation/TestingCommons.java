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

package org.apache.dubbo.annotation;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;

import static java.util.Arrays.asList;

/**
 * Common code of annotation processor testing.
 */
public final class TestingCommons {
    private TestingCommons() {
        throw new UnsupportedOperationException("No instance of TestingCommons for you! ");
    }

    private static class ObjectHolders {
        static final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        static final StandardJavaFileManager javaFileManager = javaCompiler.getStandardFileManager(
            null,
            Locale.ROOT,
            StandardCharsets.UTF_8
        );
    }

    public static boolean compileTheSource(String filePath) {


        JavaCompiler.CompilationTask compilationTask = ObjectHolders.javaCompiler.getTask(
            null,
            ObjectHolders.javaFileManager,
            null,
            asList("-parameters", "-Xlint:unchecked", "-nowarn", "-Xlint:deprecation"),
            null,
            getSourceFileJavaFileObject(filePath)
        );

        compilationTask.setProcessors(
            Collections.singletonList(new DispatchingAnnotationProcessor())
        );

        return compilationTask.call();
    }

    private static Iterable<? extends JavaFileObject> getSourceFileJavaFileObject(String filePath) {

        return ObjectHolders.javaFileManager.getJavaFileObjects(filePath);
    }
}
