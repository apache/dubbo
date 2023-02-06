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

package org.apache.dubbo.annotation.handler;

import org.apache.dubbo.annotation.AnnotationProcessingHandler;
import org.apache.dubbo.annotation.AnnotationProcessorContext;
import org.apache.dubbo.annotation.util.ASTUtils;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.ListBuffer;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles @Deprecated annotation and adds logger warn call to the methods that are annotated with it.
 */
public class DeprecatedHandler implements AnnotationProcessingHandler {

    @Override
    public Set<Class<? extends Annotation>> getAnnotationsToHandle() {
        return new HashSet<>(
            Collections.singletonList(Deprecated.class)
        );
    }

    @Override
    public void process(Set<Element> elements, AnnotationProcessorContext apContext) {
        for (Element element : elements) {
            // Only interested in methods.
            if (!(element instanceof Symbol.MethodSymbol)) {
                continue;
            }

            Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) element.getEnclosingElement();

            ASTUtils.addImportStatement(apContext, classSymbol, "org.apache.dubbo.common.logger", "LoggerFactory");
            ASTUtils.addImportStatement(apContext, classSymbol, "org.apache.dubbo.common.logger", "ErrorTypeAwareLogger");

            JCTree classTree = apContext.getJavacTrees().getTree(classSymbol);
            JCTree methodTree = apContext.getJavacTrees().getTree(element);

            methodTree.accept(new TreeTranslator() {
                @Override
                public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {

                    // Add a statement like this:
                    // LoggerFactory.getErrorTypeAwareLogger(XXX.class).warn("0-X", "", "", "....");

                    JCTree.JCBlock block = jcMethodDecl.body;

                    if (block == null) {
                        // No method body.
                        return;
                    }

                    JCTree.JCExpression getLoggerStatement = apContext.getTreeMaker().Apply(
                        // Use definite name to distinguish the java.util.List.
                        com.sun.tools.javac.util.List.nil(),

                        apContext.getTreeMaker().Select(
                            apContext.getTreeMaker().Ident(apContext.getNames().fromString("LoggerFactory")),
                            apContext.getNames().fromString("getErrorTypeAwareLogger")
                        ),

                        com.sun.tools.javac.util.List.of(
                            apContext.getTreeMaker().ClassLiteral(
                                classSymbol.erasure(
                                    Types.instance(apContext.getJavacContext())
                                )
                            )
                        )
                    );

                    JCTree.JCExpression fullStatement = apContext.getTreeMaker().Apply(
                        com.sun.tools.javac.util.List.nil(),

                        apContext.getTreeMaker().Select(
                            getLoggerStatement,
                            apContext.getNames().fromString("warn")
                        ),

                        com.sun.tools.javac.util.List.of(
                            apContext.getTreeMaker().Literal("0-28"),
                            apContext.getTreeMaker().Literal("invocation of deprecated method"),
                            apContext.getTreeMaker().Literal(""),
                            apContext.getTreeMaker().Literal("Deprecated method invoked. The method is "
                                + classSymbol.getQualifiedName() + "."
                                + jcMethodDecl.name.toString() + "(" + jcMethodDecl.params.toString() + ")")
                        )
                    );

                    JCTree.JCExpressionStatement fullExpressionStatement = apContext.getTreeMaker().Exec(fullStatement);

                    ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
                    statements.add(fullExpressionStatement);
                    statements.addAll(block.stats);

                    block.stats = statements.toList();
                }
            });
        }
    }
}
