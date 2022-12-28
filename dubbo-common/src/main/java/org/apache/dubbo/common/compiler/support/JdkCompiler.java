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
package org.apache.dubbo.common.compiler.support;


import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JdkCompiler. (SPI, Singleton, ThreadSafe)
 */
public class JdkCompiler extends AbstractCompiler {

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    private final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();

    private final ClassLoaderImpl classLoader;

    private final JavaFileManagerImpl javaFileManager;

    private final List<String> options;

    private static final String DEFAULT_JAVA_VERSION = "1.8";

    private static List<String> buildDefaultOptions(String javaVersion) {
        return Arrays.asList(
                "-source", javaVersion, "-target", javaVersion
        );
    }

    private static List<String> buildDefaultOptions() {
        return buildDefaultOptions(DEFAULT_JAVA_VERSION);
    }

    public JdkCompiler(List<String> options) {
        this.options = new ArrayList<>(options);
        StandardJavaFileManager manager = compiler.getStandardFileManager(diagnosticCollector, null, null);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader instanceof URLClassLoader
                && (!"sun.misc.Launcher$AppClassLoader".equals(loader.getClass().getName()))) {
            try {
                URLClassLoader urlClassLoader = (URLClassLoader) loader;
                List<File> files = new ArrayList<>();
                for (URL url : urlClassLoader.getURLs()) {
                    files.add(new File(url.getFile()));
                }
                manager.setLocation(StandardLocation.CLASS_PATH, files);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        classLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoaderImpl>() {
            @Override
            public ClassLoaderImpl run() {
                return new ClassLoaderImpl(loader);
            }
        });
        javaFileManager = new JavaFileManagerImpl(manager, classLoader);
    }

    public JdkCompiler() {
        this(buildDefaultOptions());
    }

    public JdkCompiler(String javaVersion) {
        this(buildDefaultOptions(javaVersion));
    }

    @Override
    public Class<?> doCompile(ClassLoader ignored, String name, String sourceCode) throws Throwable {
        int i = name.lastIndexOf('.');
        String packageName = i < 0 ? "" : name.substring(0, i);
        String className = i < 0 ? name : name.substring(i + 1);
        JavaFileObjectImpl javaFileObject = new JavaFileObjectImpl(className, sourceCode);
        javaFileManager.putFileForInput(StandardLocation.SOURCE_PATH, packageName,
                className + ClassUtils.JAVA_EXTENSION, javaFileObject);
        Boolean result = compiler.getTask(null, javaFileManager, diagnosticCollector, options,
                null, Collections.singletonList(javaFileObject)).call();
        if (result == null || !result) {
            throw new IllegalStateException("Compilation failed. class: " + name + ", diagnostics: "
                + diagnosticCollector.getDiagnostics());
        }
        return classLoader.loadClass(name);
    }

    private static final class JavaFileObjectImpl extends SimpleJavaFileObject {

        private final CharSequence source;
        private ByteArrayOutputStream bytecode;

        public JavaFileObjectImpl(final String baseName, final CharSequence source) {
            super(ClassUtils.toURI(baseName + ClassUtils.JAVA_EXTENSION), Kind.SOURCE);
            this.source = source;
        }

        JavaFileObjectImpl(final String name, final Kind kind) {
            super(ClassUtils.toURI(name), kind);
            source = null;
        }

        public JavaFileObjectImpl(URI uri, Kind kind) {
            super(uri, kind);
            source = null;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws UnsupportedOperationException {
            if (source == null) {
                throw new UnsupportedOperationException("source == null");
            }
            return source;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(getByteCode());
        }

        @Override
        public OutputStream openOutputStream() {
            return bytecode = new ByteArrayOutputStream();
        }

        public byte[] getByteCode() {
            return bytecode.toByteArray();
        }
    }

    private static final class JavaFileManagerImpl extends ForwardingJavaFileManager<JavaFileManager> {

        private final ClassLoaderImpl classLoader;

        private final Map<URI, JavaFileObject> fileObjects = new HashMap<>();

        public JavaFileManagerImpl(JavaFileManager fileManager, ClassLoaderImpl classLoader) {
            super(fileManager);
            this.classLoader = classLoader;
        }

        @Override
        public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
            FileObject o = fileObjects.get(uri(location, packageName, relativeName));
            if (o != null) {
                return o;
            }
            return super.getFileForInput(location, packageName, relativeName);
        }

        public void putFileForInput(StandardLocation location, String packageName, String relativeName, JavaFileObject file) {
            fileObjects.put(uri(location, packageName, relativeName), file);
        }

        private URI uri(Location location, String packageName, String relativeName) {
            return ClassUtils.toURI(location.getName() + '/' + packageName + '/' + relativeName);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String qualifiedName, Kind kind, FileObject outputFile)
                throws IOException {
            JavaFileObject file = new JavaFileObjectImpl(qualifiedName, kind);
            classLoader.add(qualifiedName, file);
            return file;
        }

        @Override
        public ClassLoader getClassLoader(JavaFileManager.Location location) {
            return classLoader;
        }

        @Override
        public String inferBinaryName(Location loc, JavaFileObject file) {
            if (file instanceof JavaFileObjectImpl) {
                return file.getName();
            }
            return super.inferBinaryName(loc, file);
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse)
                throws IOException {
            Iterable<JavaFileObject> result = super.list(location, packageName, kinds, recurse);

            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

            ArrayList<JavaFileObject> files = new ArrayList<>();

            if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {
                for (JavaFileObject file : fileObjects.values()) {
                    if (file.getKind() == Kind.CLASS && file.getName().startsWith(packageName)) {
                        files.add(file);
                    }
                }

                files.addAll(classLoader.files());
            } else if (location == StandardLocation.SOURCE_PATH && kinds.contains(JavaFileObject.Kind.SOURCE)) {
                for (JavaFileObject file : fileObjects.values()) {
                    if (file.getKind() == Kind.SOURCE && file.getName().startsWith(packageName)) {
                        files.add(file);
                    }
                }
            }

            for (JavaFileObject file : result) {
                files.add(file);
            }

            return files;
        }
    }

    private static final class ClassLoaderImpl extends ClassLoader {

        private final Map<String, JavaFileObject> classes = new HashMap<>();

        ClassLoaderImpl(final ClassLoader parentClassLoader) {
            super(parentClassLoader);
        }

        Collection<JavaFileObject> files() {
            return Collections.unmodifiableCollection(classes.values());
        }

        @Override
        protected Class<?> findClass(final String qualifiedClassName) throws ClassNotFoundException {
            JavaFileObject file = classes.get(qualifiedClassName);
            if (file != null) {
                byte[] bytes = ((JavaFileObjectImpl) file).getByteCode();
                return defineClass(qualifiedClassName, bytes, 0, bytes.length);
            }
            try {
                return org.apache.dubbo.common.utils.ClassUtils.forNameWithCallerClassLoader(qualifiedClassName, getClass());
            } catch (ClassNotFoundException nf) {
                return super.findClass(qualifiedClassName);
            }
        }

        void add(final String qualifiedClassName, final JavaFileObject javaFile) {
            classes.put(qualifiedClassName, javaFile);
        }

        @Override
        protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }

        @Override
        public InputStream getResourceAsStream(final String name) {
            if (name.endsWith(ClassUtils.CLASS_EXTENSION)) {
                String qualifiedClassName = name.substring(0, name.length() - ClassUtils.CLASS_EXTENSION.length()).replace('/', '.');
                JavaFileObjectImpl file = (JavaFileObjectImpl) classes.get(qualifiedClassName);
                if (file != null) {
                    return new ByteArrayInputStream(file.getByteCode());
                }
            }
            return super.getResourceAsStream(name);
        }
    }


}
