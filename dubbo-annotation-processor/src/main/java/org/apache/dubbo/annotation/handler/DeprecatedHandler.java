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
import org.apache.dubbo.annotation.constant.DeprecatedHandlerConstants;
import org.apache.dubbo.annotation.util.ASTUtils;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
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

                    // JCTree.JCExpressionStatement fullExpressionStatement = generateLoggerStatement(jcMethodDecl, apContext, classSymbol);

                    insertLoggerInvocation(block, jcMethodDecl, generateCountingStatement(apContext, classSymbol, jcMethodDecl));
                }
            });
        }
    }

    /**
     * Generate an expression statement like this:
     * <code>LoggerFactory.getErrorTypeAwareLogger(XXX.class).warn("0-X", "", "", "....");
     *
     * @param originalMethodDecl the method declaration that will add logger statement
     * @param apContext annotation processor context
     * @param classSymbol the enclosing class that will be the logger's name
     * @return generated expression statement
     */
    private JCTree.JCExpressionStatement generateLoggerStatement(JCTree.JCMethodDecl originalMethodDecl,
                                                                        AnnotationProcessorContext apContext,
                                                                        Symbol.ClassSymbol classSymbol) {

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
                apContext.getTreeMaker().Literal(DeprecatedHandlerConstants.ERROR_CODE),
                apContext.getTreeMaker().Literal(DeprecatedHandlerConstants.POSSIBLE_CAUSE),
                apContext.getTreeMaker().Literal(DeprecatedHandlerConstants.EXTENDED_MESSAGE),
                apContext.getTreeMaker().Literal("Deprecated method invoked. The method is "
                    + getMethodDefinition(classSymbol, originalMethodDecl))
            )
        );

        return apContext.getTreeMaker().Exec(fullStatement);
    }

    private String getMethodDefinition(Symbol.ClassSymbol classSymbol, JCTree.JCMethodDecl originalMethodDecl) {
        return classSymbol.getQualifiedName() + "."
            + originalMethodDecl.name.toString() + "(" + originalMethodDecl.params.toString() + ")";
    }

    /**
     * Insert logger method invocation that generated from generateLoggerStatement(...).
     *
     * @param block the method body
     * @param originalMethodDecl the method declaration that will add logger statement
     * @param fullExpressionStatement the logger invocation generated from generateLoggerStatement(...).
     */
    private void insertLoggerInvocation(JCTree.JCBlock block,
                                               JCTree.JCMethodDecl originalMethodDecl,
                                               JCTree.JCStatement fullExpressionStatement) {

        boolean isConstructor = originalMethodDecl.name.toString().equals("<init>");
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();

        // In constructor, super(...) or this(...) should be the first statement.

        if (isConstructor && !block.stats.isEmpty()) {

            boolean startsWithSuper = block.stats.get(0).toString().startsWith("super(");
            boolean startsWithThis = block.stats.get(0).toString().startsWith("this(");

            if (startsWithSuper || startsWithThis) {
                statements.add(block.stats.get(0));
                statements.add(fullExpressionStatement);
                statements.addAll(block.stats.subList(1, block.stats.size()));
            } else {
                statements.add(fullExpressionStatement);
                statements.addAll(block.stats);
            }

        } else {
            statements.add(fullExpressionStatement);
            statements.addAll(block.stats);
        }

        block.stats = statements.toList();
    }

    /**
        Generate a statement like this:

     <pre><code>
        if (!DeprecatedMethodInvocationCounter.hasThisMethodInvoked("")) {
            // logger statements.
            DeprecatedMethodInvocationCounter.increaseInvocationCount("");
        } else {
            DeprecatedMethodInvocationCounter.increaseInvocationCount("");
        }
     </code></pre>
     */
    private JCTree.JCIf generateCountingStatement(AnnotationProcessorContext apContext,
                   Symbol.ClassSymbol classSymbol,
                   JCTree.JCMethodDecl originalMethodDecl) {


        TreeMaker treeMaker = apContext.getTreeMaker();

        JCTree.JCExpression conditionStatement = treeMaker.Unary(JCTree.Tag.NOT, treeMaker.Apply(
            // Use definite name to distinguish the java.util.List.
            com.sun.tools.javac.util.List.nil(),

            apContext.getTreeMaker().Select(
                apContext.getTreeMaker().Ident(apContext.getNames().fromString("DeprecatedMethodInvocationCounter")),
                apContext.getNames().fromString("hasThisMethodInvoked")
            ),

            com.sun.tools.javac.util.List.of(
                apContext.getTreeMaker().Literal(
                    getMethodDefinition(classSymbol, originalMethodDecl)
                )
            )
        ));

        JCTree.JCExpression increaseCountStatement = treeMaker.Apply(
            // Use definite name to distinguish the java.util.List.
            com.sun.tools.javac.util.List.nil(),

            apContext.getTreeMaker().Select(
                apContext.getTreeMaker().Ident(apContext.getNames().fromString("DeprecatedMethodInvocationCounter")),
                apContext.getNames().fromString("increaseInvocationCount")
            ),

            com.sun.tools.javac.util.List.of(
                apContext.getTreeMaker().Literal(
                    getMethodDefinition(classSymbol, originalMethodDecl)
                )
            )
        );


        JCTree.JCIf ifStatement = treeMaker.If(
            conditionStatement,

            treeMaker.Block(
                Flags.BLOCK,
                List.of(
                    generateLoggerStatement(originalMethodDecl, apContext, classSymbol),
                    treeMaker.Exec(increaseCountStatement)
                )
            ), treeMaker.Exec(increaseCountStatement)
        );

        return ifStatement;
    }
}
