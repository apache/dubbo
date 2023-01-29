package org.apache.dubbo.annotation.handler;

import org.apache.dubbo.annotation.AnnotationProcessingHandler;
import org.apache.dubbo.annotation.AnnotationProcessorContext;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.ListBuffer;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Info here.
 *
 * @author Andy Cheung
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

            addImportStatement(apContext, classSymbol, "org.apache.dubbo.common.logger", "LoggerFactory");
            addImportStatement(apContext, classSymbol, "org.apache.dubbo.common.logger", "ErrorTypeAwareLogger");

            JCTree classTree = apContext.getJavacTrees().getTree(classSymbol);
            JCTree methodTree = apContext.getJavacTrees().getTree(element);

            methodTree.accept(new TreeTranslator() {
                @Override
                public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {

                    // Add a statement like this:
                    // LoggerFactory.getErrorTypeAwareLogger(XXX.class).warn("0-X", "", "", "....");

                    JCTree.JCBlock block = jcMethodDecl.body;

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

    private void addImportStatement(AnnotationProcessorContext apContext,
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

        // ((JCTree.JCImport) ((List) imports).get(7)).qualid.toString()
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
}
