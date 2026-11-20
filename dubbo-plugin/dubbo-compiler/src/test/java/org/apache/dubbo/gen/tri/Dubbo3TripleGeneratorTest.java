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

import java.util.List;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Dubbo3TripleGeneratorTest {

    @Test
    void generateVoidMethodForGoogleProtobufEmptyOutput() {
        List<CodeGeneratorResponse.File> files = new Dubbo3TripleGenerator().generateFiles(emptyOutputRequest());

        String interfaceContent =
                findFile(files, "org/apache/dubbo/test/EmptyService.java").getContent();
        String stubContent = findFile(files, "org/apache/dubbo/test/DubboEmptyServiceTriple.java")
                .getContent();

        assertTrue(interfaceContent.contains("void ack("), interfaceContent);
        assertTrue(
                interfaceContent.contains("CompletableFuture<com.google.protobuf.Empty> ackAsync("), interfaceContent);
        assertTrue(stubContent.contains("public void ack("), stubContent);
        assertTrue(stubContent.contains("StubInvocationUtil.unaryCall(invoker, ackMethod, request);"));
        assertTrue(stubContent.contains("com.google.protobuf.Empty::parseFrom"));
        assertTrue(stubContent.contains("syncToAsyncVoid(this::ack"));
        assertTrue(stubContent.contains(
                "CompletableFuture.completedFuture(com.google.protobuf.Empty.getDefaultInstance());"));
    }

    @Test
    void generateMessageReturnMethodForNormalUnaryOutput() {
        List<CodeGeneratorResponse.File> files = new Dubbo3TripleGenerator().generateFiles(normalOutputRequest());

        String interfaceContent =
                findFile(files, "org/apache/dubbo/test/EchoService.java").getContent();
        String stubContent = findFile(files, "org/apache/dubbo/test/DubboEchoServiceTriple.java")
                .getContent();

        assertTrue(interfaceContent.contains("org.apache.dubbo.test.SampleResponse echo("), interfaceContent);
        assertTrue(
                interfaceContent.contains("CompletableFuture<org.apache.dubbo.test.SampleResponse> echoAsync("),
                interfaceContent);
        assertTrue(stubContent.contains("public org.apache.dubbo.test.SampleResponse echo("), stubContent);
        assertTrue(stubContent.contains("return StubInvocationUtil.unaryCall(invoker, echoMethod, request);"));
        assertFalse(stubContent.contains("syncToAsyncVoid("), stubContent);
    }

    private CodeGeneratorResponse.File findFile(List<CodeGeneratorResponse.File> files, String name) {
        return files.stream()
                .filter(file -> name.equals(file.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Generated file not found: " + name));
    }

    private CodeGeneratorRequest emptyOutputRequest() {
        FileDescriptorProto sampleProto = FileDescriptorProto.newBuilder()
                .setName("sample.proto")
                .setPackage("org.apache.dubbo.test")
                .addDependency("google/protobuf/empty.proto")
                .setOptions(FileOptions.newBuilder()
                        .setJavaPackage("org.apache.dubbo.test")
                        .setJavaMultipleFiles(true))
                .addMessageType(DescriptorProto.newBuilder().setName("SampleRequest"))
                .addService(ServiceDescriptorProto.newBuilder()
                        .setName("EmptyService")
                        .addMethod(MethodDescriptorProto.newBuilder()
                                .setName("Ack")
                                .setInputType(".org.apache.dubbo.test.SampleRequest")
                                .setOutputType(".google.protobuf.Empty")))
                .build();

        return CodeGeneratorRequest.newBuilder()
                .addFileToGenerate("sample.proto")
                .addProtoFile(sampleProto)
                .build();
    }

    private CodeGeneratorRequest normalOutputRequest() {
        FileDescriptorProto sampleProto = FileDescriptorProto.newBuilder()
                .setName("normal.proto")
                .setPackage("org.apache.dubbo.test")
                .setOptions(FileOptions.newBuilder()
                        .setJavaPackage("org.apache.dubbo.test")
                        .setJavaMultipleFiles(true))
                .addMessageType(DescriptorProto.newBuilder().setName("SampleRequest"))
                .addMessageType(DescriptorProto.newBuilder().setName("SampleResponse"))
                .addService(ServiceDescriptorProto.newBuilder()
                        .setName("EchoService")
                        .addMethod(MethodDescriptorProto.newBuilder()
                                .setName("Echo")
                                .setInputType(".org.apache.dubbo.test.SampleRequest")
                                .setOutputType(".org.apache.dubbo.test.SampleResponse")))
                .build();

        return CodeGeneratorRequest.newBuilder()
                .addFileToGenerate("normal.proto")
                .addProtoFile(sampleProto)
                .build();
    }
}
