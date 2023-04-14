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
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

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

            ASTUtils.addImportStatement(apContext, classSymbol, "org.apache.dubbo.common", "DeprecatedMethodInvocationCounter");

            JCTree methodTree = apContext.getJavacTrees().getTree(element);

            methodTree.accept(new TreeTranslator() {
                @Override
                public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {

                    JCTree.JCBlock block = jcMethodDecl.body;

                    if (block == null) {
                        // No method body. (i.e. interface method declaration.)
                        return;
                    }

                    ASTUtils.insertStatementToHeadOfMethod(block, jcMethodDecl, generateCounterStatement(apContext, classSymbol, jcMethodDecl));
                }
            });
        }
    }

    /**
     * Generate an expression statement like this:
     * <code>DeprecatedMethodInvocationCounter.onDeprecatedMethodCalled("....");
     *
     * @param originalMethodDecl the method declaration that will add logger statement
     * @param apContext annotation processor context
     * @param classSymbol the enclosing class that will be the logger's name
     * @return generated expression statement
     */
    private JCTree.JCExpressionStatement generateCounterStatement(AnnotationProcessorContext apContext,
                                                                  Symbol.ClassSymbol classSymbol,
                                                                  JCTree.JCMethodDecl originalMethodDecl) {

        JCTree.JCExpression fullStatement = apContext.getTreeMaker().Apply(
            com.sun.tools.javac.util.List.nil(),

            apContext.getTreeMaker().Select(
                apContext.getTreeMaker().Ident(apContext.getNames().fromString("DeprecatedMethodInvocationCounter")),
                apContext.getNames().fromString("onDeprecatedMethodCalled")
            ),

            com.sun.tools.javac.util.List.of(
                apContext.getTreeMaker().Literal(getMethodDefinition(classSymbol, originalMethodDecl))
            )
        );

        return apContext.getTreeMaker().Exec(fullStatement);
    }

    private String getMethodDefinition(Symbol.ClassSymbol classSymbol, JCTree.JCMethodDecl originalMethodDecl) {
        return classSymbol.getQualifiedName() + "."
            + originalMethodDecl.name.toString() + "(" + originalMethodDecl.params.toString() + ")";
    }
}
