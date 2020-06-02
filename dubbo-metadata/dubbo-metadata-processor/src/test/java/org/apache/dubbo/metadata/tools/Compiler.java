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
package org.apache.dubbo.metadata.tools;

import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * The Java Compiler
 */
public class Compiler {

    private final File sourceDirectory;

    private final JavaCompiler javaCompiler;

    private final StandardJavaFileManager javaFileManager;

    private final Set<Processor> processors = new LinkedHashSet<>();

    public Compiler() throws IOException {
        this(defaultTargetDirectory());
    }

    public Compiler(File targetDirectory) throws IOException {
        this(defaultSourceDirectory(), targetDirectory);
    }

    public Compiler(File sourceDirectory, File targetDirectory) throws IOException {
        this.sourceDirectory = sourceDirectory;
        this.javaCompiler = ToolProvider.getSystemJavaCompiler();
        this.javaFileManager = javaCompiler.getStandardFileManager(null, null, null);
        this.javaFileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(targetDirectory));
        this.javaFileManager.setLocation(StandardLocation.SOURCE_OUTPUT, Collections.singleton(targetDirectory));
    }

    private static File defaultSourceDirectory() {
        return new File(defaultRootDirectory(), "src/test/java");
    }

    private static File defaultRootDirectory() {
        return detectClassPath(Compiler.class).getParentFile().getParentFile();
    }

    private static File defaultTargetDirectory() {
        File dir = new File(defaultRootDirectory(), "target/generated-classes");
        dir.mkdirs();
        return dir;
    }

    private static File detectClassPath(Class<?> targetClass) {
        URL classFileURL = targetClass.getProtectionDomain().getCodeSource().getLocation();
        if ("file".equals(classFileURL.getProtocol())) {
            return new File(classFileURL.getPath());
        } else {
            throw new RuntimeException("No support");
        }
    }

    public Compiler processors(Processor... processors) {
        this.processors.addAll(asList(processors));
        return this;
    }

    private Iterable<? extends JavaFileObject> getJavaFileObjects(Class<?>... sourceClasses) {
        int size = sourceClasses == null ? 0 : sourceClasses.length;
        File[] javaSourceFiles = new File[size];
        for (int i = 0; i < size; i++) {
            File javaSourceFile = javaSourceFile(sourceClasses[i].getName());
            javaSourceFiles[i] = javaSourceFile;
        }
        return javaFileManager.getJavaFileObjects(javaSourceFiles);
    }

    private File javaSourceFile(String sourceClassName) {
        String javaSourceFilePath = sourceClassName.replace('.', '/').concat(".java");
        return new File(sourceDirectory, javaSourceFilePath);
    }

    public boolean compile(Class<?>... sourceClasses) {
        JavaCompiler.CompilationTask task = javaCompiler.getTask(null, this.javaFileManager, null,
                asList("-parameters", "-Xlint:unchecked", "-nowarn", "-Xlint:deprecation"),
//                null,
                null, getJavaFileObjects(sourceClasses));
        if (!processors.isEmpty()) {
            task.setProcessors(processors);
        }
        return task.call();
    }

    public JavaCompiler getJavaCompiler() {
        return javaCompiler;
    }
}
