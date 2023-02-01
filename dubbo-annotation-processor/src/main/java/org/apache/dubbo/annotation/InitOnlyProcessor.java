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

import org.apache.dubbo.annotation.permit.Permit;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dispatching annotation processor, which uses a file to locate handlers and invoke them.
 */
@SupportedAnnotationTypes("*")
public class InitOnlyProcessor extends AbstractProcessor {

    private static Class<DispatchingAnnotationProcessor> dapClass;

    private static DispatchingAnnotationProcessor dap;

    private static final AtomicReference<ClassLoader> CLASS_LOADER = new AtomicReference<>(null);

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        try {
            ClassLoader cl = findAndPatchClassLoader(processingEnv);
            dapClass = (Class<DispatchingAnnotationProcessor>) cl.loadClass("org.apache.dubbo.annotation.DispatchingAnnotationProcessor");
            dap = dapClass.getConstructor().newInstance();

            dap.init(processingEnv);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        dap.process(annotations, roundEnv);

        return false;
    }

    private static String urlDecode(String in, boolean forceUtf8) {
        try {
            return URLDecoder.decode(in, forceUtf8 ? "UTF-8" : Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException e) {
            try {
                return URLDecoder.decode(in, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                return in;
            }
        }
    }

    public static String findClassRootOfClass(Class<?> context) {
        String name = context.getName();
        int idx = name.lastIndexOf('.');
        String packageBase;
        if (idx > -1) {
            packageBase = name.substring(0, idx);
            name = name.substring(idx + 1);
        } else {
            packageBase = "";
        }

        URL selfURL = context.getResource(name + ".class");
        String self = selfURL.toString();
        if (self.startsWith("file:/"))  {
            String path = urlDecode(self.substring(5), false);
            if (!new File(path).exists()) path = urlDecode(self.substring(5), true);
            String suffix = "/" + packageBase.replace('.', '/') + "/" + name + ".class";
            if (!path.endsWith(suffix)) throw new IllegalArgumentException("Unknown path structure: " + path);

            self = path.substring(0, path.length() - suffix.length());
        } else if (self.startsWith("jar:")) {
            int sep = self.indexOf('!');
            if (sep == -1) throw new IllegalArgumentException("No separator in jar protocol: " + self);
            String jarLoc = self.substring(4, sep);
            if (jarLoc.startsWith("file:/")) {
                String path = urlDecode(jarLoc.substring(5), false);
                if (!new File(path).exists()) path = urlDecode(jarLoc.substring(5), true);
                self = path;
            } else throw new IllegalArgumentException("Unknown path structure: " + self);
        } else {
            throw new IllegalArgumentException("Unknown protocol: " + self);
        }

        if (self.isEmpty()) self = "/";

        return self;
    }

    private static ClassLoader findAndPatchClassLoader(ProcessingEnvironment procEnv) throws Exception {
        ClassLoader environmentClassLoader = procEnv.getClass().getClassLoader();
        if (environmentClassLoader != null && environmentClassLoader.getClass().getCanonicalName().equals("org.codehaus.plexus.compiler.javac.IsolatedClassLoader")) {
            if (CLASS_LOADER.getAndSet(environmentClassLoader) != null) {
                Method m = Permit.getMethod(environmentClassLoader.getClass(), "addURL", URL.class);
                URL selfUrl = new File(findClassRootOfClass(InitOnlyProcessor.class)).toURI().toURL();
                Permit.invoke(m, environmentClassLoader, selfUrl);
            }
        }

        ClassLoader ourClassLoader = DispatchingAnnotationProcessor.class.getClassLoader();
        if (ourClassLoader == null) return ClassLoader.getSystemClassLoader();
        return ourClassLoader;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
