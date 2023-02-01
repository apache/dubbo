package org.apache.dubbo.annotation;

import org.apache.dubbo.annotation.util.FileUtils;
import org.apache.dubbo.eci.extractor.ErrorCodeExtractor;
import org.apache.dubbo.eci.extractor.JavassistConstantPoolErrorCodeExtractor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;

/**
 * Real invocation test of DispatchingAnnotationProcessor (and DeprecatedHandler).
 */
public class RealInvocationTest {

    private static final String filePath = FileUtils.getResourceFilePath("org/testing/dm/TestDeprecatedMethod.java");

    private static Iterable<? extends JavaFileObject> getSourceFileJavaFileObject(StandardJavaFileManager javaFileManager) {
        return javaFileManager.getJavaFileObjects(filePath);
    }

    @BeforeAll
    public static void compileTheSource() {
        Path classPath = Paths.get(filePath.replace(".java", ".class"));

        if (Files.exists(classPath)) {
            try {
                Files.delete(classPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager javaFileManager = javaCompiler.getStandardFileManager(
            null,
            Locale.ROOT,
            StandardCharsets.UTF_8
        );

        JavaCompiler.CompilationTask compilationTask = javaCompiler.getTask(
            null,
            javaFileManager,
            null,
            asList("-parameters", "-Xlint:unchecked", "-nowarn", "-Xlint:deprecation"),
            null,
            getSourceFileJavaFileObject(javaFileManager)
        );

        compilationTask.setProcessors(
            Collections.singletonList(new InitOnlyProcessor())
        );

        compilationTask.call();
    }

    @Test
    void test() {
        ErrorCodeExtractor errorCodeExtractor = new JavassistConstantPoolErrorCodeExtractor();
        List<String> codes = errorCodeExtractor.getErrorCodes(filePath.replace(".java", ".class"));

        Assertions.assertTrue(codes.contains("0-28"));
    }
}
