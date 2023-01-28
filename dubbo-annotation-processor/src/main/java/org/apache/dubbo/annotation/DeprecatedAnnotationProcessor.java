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

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Info here.
 *
 * @author Andy Cheung
 */
@SupportedAnnotationTypes("*")
public class DeprecatedAnnotationProcessor extends AbstractProcessor {

    private static final Set<Class<? extends Annotation>> ANNOTATIONS_TO_HANDLE = new HashSet<>(
        Collections.singletonList(Deprecated.class)
    );

    private JavacTrees javacTrees;

    private TreeMaker treeMaker;

    private Names names;

    private Context javacContext;

    private static <T> T jbUnwrap(Class<? extends T> iface, T wrapper) {
        T unwrapped = null;
        try {
            final Class<?> apiWrappers = wrapper.getClass().getClassLoader().loadClass("org.jetbrains.jps.javac.APIWrappers");
            final Method unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
            unwrapped = iface.cast(unwrapMethod.invoke(null, iface, wrapper));
        } catch (Throwable ignored) {
        }

        return unwrapped != null ? unwrapped : wrapper;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        Permit.addOpens();

        super.init(processingEnv);

        Object procEnvToUnwrap = processingEnv.getClass() == JavacProcessingEnvironment.class ?
            processingEnv : jbUnwrap(JavacProcessingEnvironment.class, processingEnv);

        JavacProcessingEnvironment jcProcessingEnvironment = (JavacProcessingEnvironment) procEnvToUnwrap;

        Context context = jcProcessingEnvironment.getContext();

        javacContext = context;
        javacTrees = JavacTrees.instance(jcProcessingEnvironment);
        treeMaker = TreeMaker.instance(context);
        names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.err.println("Hello AP! ");
        Set<Element> elements = new HashSet<>(32);

        for (Class<? extends Annotation> annotationClass : ANNOTATIONS_TO_HANDLE) {
            elements.addAll(roundEnv.getElementsAnnotatedWith(annotationClass));
        }

        for (Element element : elements) {
            // Only interested in methods.
            if (!(element instanceof Symbol.MethodSymbol)) {
                continue;
            }

            Element classSymbol = element.getEnclosingElement();

            JCTree classTree = javacTrees.getTree(classSymbol);
            JCTree methodTree = javacTrees.getTree(element);

            methodTree.accept(new TreeTranslator() {
                @Override
                public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {

                    // Add a statement like this:
                    // LoggerFactory.getErrorTypeAwareLogger(XXX.class).warn("0-X", "", "", "....");

                    JCTree.JCBlock block = jcMethodDecl.body;

                    JCTree.JCExpressionStatement getLoggerStatement = treeMaker.Exec(
                        treeMaker.Apply(
                            // Use definite name to distinguish the java.util.List.
                            com.sun.tools.javac.util.List.nil(),

                            treeMaker.Select(
                                treeMaker.Ident(names.fromString("LoggerFactory")),
                                names.fromString("getErrorTypeAwareLogger")
                            ),

                            com.sun.tools.javac.util.List.of(
                                treeMaker.ClassLiteral(((Symbol.ClassSymbol) classSymbol).erasure(Types.instance(javacContext)))
                            )
                        )
                    );

                    ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
                    statements.add(getLoggerStatement);
                    statements.addAll(block.stats);

                    block.stats = statements.toList();
                }
            });
        }

        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
