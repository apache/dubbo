package org.apache.dubbo.annotation;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;

import static java.util.Arrays.asList;

/**
 * Info here.
 *
 * @author Andy Cheung
 */
public final class TestingCommons {
    private TestingCommons() {
        throw new UnsupportedOperationException("No instance of TestingCommons for you! ");
    }

    private static class ObjectHolders {
        static final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        static final StandardJavaFileManager javaFileManager = javaCompiler.getStandardFileManager(
            null,
            Locale.ROOT,
            StandardCharsets.UTF_8
        );
    }

    public static boolean compileTheSource(String filePath) {


        JavaCompiler.CompilationTask compilationTask = ObjectHolders.javaCompiler.getTask(
            null,
            ObjectHolders.javaFileManager,
            null,
            asList("-parameters", "-Xlint:unchecked", "-nowarn", "-Xlint:deprecation"),
            null,
            getSourceFileJavaFileObject(filePath)
        );

        compilationTask.setProcessors(
            Collections.singletonList(new DispatchingAnnotationProcessor())
        );

        return compilationTask.call();
    }

    private static Iterable<? extends JavaFileObject> getSourceFileJavaFileObject(String filePath) {

        return ObjectHolders.javaFileManager.getJavaFileObjects(filePath);
    }
}
