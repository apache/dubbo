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
package org.apache.dubbo.gen.tri;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Dubbo3TripleGenerator}.
 *
 * @date 2026-08-15
 */
class Dubbo3TripleGeneratorTest {

    @TempDir
    Path outputDirectory;

    @Test
    void generatedUnaryInterfaceCanBeImplementedWithSyncMethodOnly() throws IOException {
        String generatedInterface = generateGreeterServiceInterface();

        assertFalse(generatedInterface.contains("void sayHello("));

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    Arrays.asList("-d", outputDirectory.toString(), "-proc:none"),
                    null,
                    compilationSources(generatedInterface));

            assertTrue(Boolean.TRUE.equals(task.call()), formatDiagnostics(diagnostics));
        }
    }

    private String generateGreeterServiceInterface() {
        FileDescriptorProto proto = FileDescriptorProto.newBuilder()
                .setName("greeter.proto")
                .setPackage("repro")
                .setOptions(FileOptions.newBuilder().setJavaPackage("repro").setJavaMultipleFiles(true))
                .addMessageType(DescriptorProto.newBuilder().setName("HelloRequest"))
                .addMessageType(DescriptorProto.newBuilder().setName("HelloReply"))
                .addService(ServiceDescriptorProto.newBuilder()
                        .setName("GreeterService")
                        .addMethod(MethodDescriptorProto.newBuilder()
                                .setName("SayHello")
                                .setInputType(".repro.HelloRequest")
                                .setOutputType(".repro.HelloReply")))
                .build();
        CodeGeneratorRequest request = CodeGeneratorRequest.newBuilder()
                .addFileToGenerate(proto.getName())
                .addProtoFile(proto)
                .build();

        return new Dubbo3TripleGenerator()
                .generateFiles(request).stream()
                        .filter(file -> file.getName().equals("repro/GreeterService.java"))
                        .map(CodeGeneratorResponse.File::getContent)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("GreeterService interface was not generated"));
    }

    private List<JavaFileObject> compilationSources(String generatedInterface) {
        return Arrays.asList(
                source("repro.GreeterService", generatedInterface),
                source("repro.HelloRequest", "package repro; public final class HelloRequest {}"),
                source("repro.HelloReply", "package repro; public final class HelloReply {}"),
                source(
                        "repro.SyncOnlyGreeterService",
                        "package repro; public final class SyncOnlyGreeterService implements GreeterService {"
                                + " public HelloReply sayHello(HelloRequest request) { return new HelloReply(); }"
                                + "}"),
                source(
                        "repro.AsyncGreeterService",
                        "package repro;"
                                + " import java.util.concurrent.CompletableFuture;"
                                + " public final class AsyncGreeterService implements GreeterService {"
                                + " public HelloReply sayHello(HelloRequest request) { return new HelloReply(); }"
                                + " @Override public CompletableFuture<HelloReply>"
                                + " sayHelloAsync(HelloRequest request) {"
                                + " return CompletableFuture.completedFuture(sayHello(request));"
                                + " }"
                                + "}"),
                source(
                        "org.apache.dubbo.common.stream.StreamObserver",
                        "package org.apache.dubbo.common.stream; public interface StreamObserver<T> {}"),
                source(
                        "org.apache.dubbo.remoting.http12.HttpMethods",
                        "package org.apache.dubbo.remoting.http12; public enum HttpMethods {"
                                + " GET, PUT, POST, DELETE, PATCH"
                                + "}"),
                source(
                        "org.apache.dubbo.remoting.http12.rest.Mapping",
                        "package org.apache.dubbo.remoting.http12.rest;"
                                + " import org.apache.dubbo.remoting.http12.HttpMethods;"
                                + " public @interface Mapping { HttpMethods method(); String path(); }"),
                source(
                        "org.apache.dubbo.rpc.stub.annotations.GRequest",
                        "package org.apache.dubbo.rpc.stub.annotations;"
                                + " public @interface GRequest { String value() default \"\"; }"),
                source(
                        "org.apache.dubbo.rpc.model.DubboStub",
                        "package org.apache.dubbo.rpc.model; public interface DubboStub {}"));
    }

    private StringJavaFileObject source(String className, String source) {
        return new StringJavaFileObject(className, source);
    }

    private String formatDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        return diagnostics.getDiagnostics().stream()
                .map(diagnostic -> diagnostic.getKind() + ": " + diagnostic.getMessage(Locale.ROOT))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static final class StringJavaFileObject extends SimpleJavaFileObject {

        private final String source;

        private StringJavaFileObject(String className, String source) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
