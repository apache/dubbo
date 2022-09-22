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

package org.apache.dubbo.errorcode.extractor;

import org.apache.dubbo.errorcode.model.FieldDefinition;
import org.apache.dubbo.errorcode.model.LoggerMethodInvocation;
import org.apache.dubbo.errorcode.util.FileUtils;

import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.dubbo.errorcode.util.CollectionUtil.mapOf;

/**
 * Locator of invalid logger invocation based on Eclipse JDT.
 */
@SuppressWarnings("unchecked")
public class JdtBasedInvalidLoggerInvocationLocator implements InvalidLoggerInvocationLocator {

    private static final String JDT_CORE_COMPILER_SOURCE_PROPERTY_KEY = "org.eclipse.jdt.core.compiler.source";

    private static final String JDT_CORE_COMPILER_SOURCE_VERSION = "17";

    @Override
    public List<LoggerMethodInvocation> locateInvalidLoggerInvocation(String classFile) {
        String loggerFieldName = getLoggerFieldName(classFile);

        Pattern pattern = Pattern.compile(loggerFieldName + "\\.(warn|error)+\\(");

        String sourceText = FileUtils.openFileAsString(FileUtils.getSourceFilePathFromClassFilePath(classFile));

        Map<String, List<Integer>> lineOfInvocation = getLineOfLoggerInvocations(pattern, sourceText);

        ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
        astParser.setSource(sourceText.toCharArray());
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);
        astParser.setCompilerOptions(
            mapOf(JDT_CORE_COMPILER_SOURCE_PROPERTY_KEY, JDT_CORE_COMPILER_SOURCE_VERSION));

        CompilationUnit n = (CompilationUnit) astParser.createAST(null);
        List<TypeDeclaration> types = n.types();
        TypeDeclaration type = types.get(0);

        List<LoggerMethodInvocation> invalidInvocations = new ArrayList<>();
        Map<String, List<Integer>> finalLineOfInvocation = lineOfInvocation;
        type.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation node) {

                Expression exp = node.getExpression();

                if (exp instanceof SimpleName) {

                    String fieldName = ((SimpleName) exp).getIdentifier();
                    String methodName = node.getName().getIdentifier();

                    if (fieldName.equals(loggerFieldName) &&
                        (methodName.equals("warn") || methodName.equals("error"))) {

                        if (node.arguments().size() < 4) {
                            LoggerMethodInvocation loggerMethodInvocation = new LoggerMethodInvocation();

                            loggerMethodInvocation.setLoggerMethodInvocationCode(node.toString());
                            loggerMethodInvocation.setOccurredLines(finalLineOfInvocation.get(node.toString()));

                            invalidInvocations.add(loggerMethodInvocation);
                        }
                    }
                }

                return super.visit(node);
            }
        });

        return invalidInvocations.stream().distinct().collect(Collectors.toList());
    }

    private static Map<String, List<Integer>> getLineOfLoggerInvocations(Pattern pattern, String sourceText) {
        Scanner scanner = new Scanner(sourceText);

        Map<String, List<Integer>> lineOfInvocation = new HashMap<>();

        ASTParser astParserInLoop = ASTParser.newParser(AST.getJLSLatest());

        String previousLine = "";

        for (int i = 0, notFullyInterpreted = 0; scanner.hasNextLine(); i++) {
            String nextLine = scanner.nextLine().trim();

            if (pattern.matcher(nextLine).find() || notFullyInterpreted == 1) {

                if (notFullyInterpreted == 1) {
                    List<Integer> prevLineData = lineOfInvocation.remove(previousLine);

                    nextLine = previousLine + nextLine;
                    lineOfInvocation.put(nextLine, prevLineData);

                    notFullyInterpreted = 0;
                }

                astParserInLoop.setSource(nextLine.toCharArray());
                astParserInLoop.setKind(ASTParser.K_STATEMENTS);
                astParserInLoop.setCompilerOptions(mapOf(JDT_CORE_COMPILER_SOURCE_PROPERTY_KEY, JDT_CORE_COMPILER_SOURCE_VERSION));

                Block ast = (Block) astParserInLoop.createAST(null);

                // Filter out comments.
                if (ast.statements().isEmpty() && !nextLine.startsWith("//")) {
                    // Syntax error, hence no statement presents.
                    notFullyInterpreted = 1;
                }

                String key = nextLine.trim();

                List<Integer> al = lineOfInvocation.putIfAbsent(key, new ArrayList<>());
                al = al == null ? lineOfInvocation.get(key) : al;
                al.add(i + 1);
            }

            previousLine = nextLine;
        }

        // Filter out comment statement.
        lineOfInvocation = lineOfInvocation
            .entrySet()
            .stream()
            .filter(e -> !e.getKey().startsWith("//"))
            .collect(Collectors.toMap(e -> {
                // Convert the source string to JDT-compatible string.

                astParserInLoop.setSource(e.getKey().toCharArray());
                astParserInLoop.setKind(ASTParser.K_STATEMENTS);
                astParserInLoop.setCompilerOptions(mapOf(JDT_CORE_COMPILER_SOURCE_PROPERTY_KEY, JDT_CORE_COMPILER_SOURCE_VERSION));

                List<ExpressionStatement> stmts = ((Block) astParserInLoop.createAST(null)).statements();

                return stmts.get(0).getExpression().toString();

            }, Map.Entry::getValue));

        return lineOfInvocation;
    }

    private static String getLoggerFieldName(String classFile) {
        ClassFile clsF = JavassistUtils.openClassFile(classFile);
        List<FieldInfo> fields = clsF.getFields();

        Predicate<FieldInfo> etaLogger = x -> x.getDescriptor().equals("Lorg/apache/dubbo/common/logger/ErrorTypeAwareLogger;");
        Predicate<FieldInfo> legacyLogger = x -> x.getDescriptor().equals("Lorg/apache/dubbo/common/logger/Logger;");
        Predicate<FieldInfo> loggerType = etaLogger.or(legacyLogger);

        List<FieldDefinition> fieldNameCollection = fields.stream()
            .filter(loggerType)
            .map(x -> {
                FieldDefinition def = new FieldDefinition();

                def.setContainerClass(clsF.getName());
                def.setFieldName(x.getName());
                def.setFieldType(x.getDescriptor());

                return def;
            }).collect(Collectors.toList());

        if (fieldNameCollection.isEmpty()) {
            // Logger field is in the super class.

            String superClass = clsF.getSuperclass();

            if (Objects.equals(superClass, Object.class.getName())) {
                return "logger";
            }

            String superClassSimpleName = superClass.substring(superClass.lastIndexOf('.') + 1);
            System.out.println(classFile);

            String classFileFolderPath = Paths.get(classFile).getParent().toString();
            String superClassClassFile = classFileFolderPath + File.separator + superClassSimpleName + ".class";

            if (!Files.exists(Paths.get(superClassClassFile))) {
                // The field is in the different module or package.

                // Since walking over the project has a really poor performance,
                // just return the most frequently used field name...

                return "logger";
            }

            return getLoggerFieldName(superClassClassFile);
        }

        return fieldNameCollection.get(0).getFieldName();
    }
}
