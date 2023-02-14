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

package org.apache.dubbo.annotation.util;

import org.apache.dubbo.annotation.AnnotationProcessorContext;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.ListBuffer;

import java.util.List;

/**
 * Some utils about AST manipulating.
 */
public final class ASTUtils {

    private ASTUtils() {
        throw new UnsupportedOperationException("No instance of 'ASTUtils' for you! ");
    }

    public static void addImportStatement(AnnotationProcessorContext apContext,
                                    Symbol.ClassSymbol classSymbol,
                                    String packageName,
                                    String className) {

        JCTree.JCImport jcImport = apContext.getTreeMaker().Import(
            apContext.getTreeMaker().Select(
                apContext.getTreeMaker().Ident(apContext.getNames().fromString(packageName)),
                apContext.getNames().fromString(className)
            ), false);

        TreePath treePath = apContext.getTrees().getPath(classSymbol);
        TreePath parentPath = treePath.getParentPath();
        JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) parentPath.getCompilationUnit();

        List<JCTree.JCImport> imports = compilationUnit.getImports();
        if (imports.stream().noneMatch(x -> x.qualid.toString().contains(packageName + "." + className))) {

            compilationUnit.accept(new JCTree.Visitor() {
                @Override
                public void visitTopLevel(JCTree.JCCompilationUnit that) {

                    List<JCTree> defs = compilationUnit.defs;

                    ListBuffer<JCTree> newDefs = new ListBuffer<>();

                    newDefs.add(defs.get(0));
                    newDefs.add(jcImport);
                    newDefs.addAll(defs.subList(1, defs.size()));

                    compilationUnit.defs = newDefs.toList();
                }
            });
        }
    }

    /**
     * Insert statement to head of the method.
     *
     * @param block the method body
     * @param originalMethodDecl the method declaration that will add logger statement
     * @param fullExpressionStatement the statement to insert.
     */
    public static void insertStatementToHeadOfMethod(JCTree.JCBlock block,
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
}
