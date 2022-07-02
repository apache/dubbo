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

package org.apache.dubbo.rpc.protocol.tri.service;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.TriRpcStatus;

import com.google.protobuf.Descriptors.FileDescriptor;
import io.grpc.reflection.v1alpha.DubboServerReflectionTriple;
import io.grpc.reflection.v1alpha.ErrorResponse;
import io.grpc.reflection.v1alpha.ExtensionNumberResponse;
import io.grpc.reflection.v1alpha.ExtensionRequest;
import io.grpc.reflection.v1alpha.FileDescriptorResponse;
import io.grpc.reflection.v1alpha.ListServiceResponse;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.reflection.v1alpha.ServiceResponse;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Provides a reflection service for Protobuf service for service test and dynamic gateway.
 *
 * @link https://github.com/grpc/grpc/blob/master/doc/server-reflection.md
 */
public class ReflectionV1AlphaService extends DubboServerReflectionTriple.ServerReflectionImplBase {


    @Override
    public StreamObserver<ServerReflectionRequest> serverReflectionInfo(
        StreamObserver<ServerReflectionResponse> responseObserver) {
        return new StreamObserver<ServerReflectionRequest>() {
            @Override
            public void onNext(ServerReflectionRequest request) {
                switch (request.getMessageRequestCase()) {
                    case FILE_BY_FILENAME:
                        getFileByName(request, responseObserver);
                        break;
                    case FILE_CONTAINING_SYMBOL:
                        getFileContainingSymbol(request, responseObserver);
                        break;
                    case FILE_CONTAINING_EXTENSION:
                        getFileByExtension(request, responseObserver);
                        break;
                    case ALL_EXTENSION_NUMBERS_OF_TYPE:
                        getAllExtensions(request, responseObserver);
                        break;
                    case LIST_SERVICES:
                        listServices(request, responseObserver);
                        break;
                    default:
                        sendErrorResponse(
                            request,
                            TriRpcStatus.Code.UNIMPLEMENTED,
                            "not implemented " + request.getMessageRequestCase(), responseObserver);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    private void getFileByName(ServerReflectionRequest request,
        StreamObserver<ServerReflectionResponse> responseObserver) {
        String name = request.getFileByFilename();
        FileDescriptor fd = SchemaDescriptorRegistry.getSchemaDescriptor(name);
        if (fd != null) {
            responseObserver.onNext(createServerReflectionResponse(request, fd));
        } else {
            sendErrorResponse(request, TriRpcStatus.Code.NOT_FOUND, "File not found.",
                responseObserver);
        }
    }

    private void getFileContainingSymbol(ServerReflectionRequest request,
        StreamObserver<ServerReflectionResponse> responseObserver) {
        String symbol = request.getFileContainingSymbol();
        FileDescriptor fd = SchemaDescriptorRegistry.getSchemaDescriptor(symbol);
        if (fd != null) {
            responseObserver.onNext(createServerReflectionResponse(request, fd));
        } else {
            sendErrorResponse(request, TriRpcStatus.Code.NOT_FOUND, "Symbol not found.",
                responseObserver);
        }
    }

    private void getFileByExtension(ServerReflectionRequest request,
        StreamObserver<ServerReflectionResponse> responseObserver) {
        ExtensionRequest extensionRequest = request.getFileContainingExtension();
        String type = extensionRequest.getContainingType();
        int extension = extensionRequest.getExtensionNumber();
        FileDescriptor fd =
            SchemaDescriptorRegistry.getFileDescriptorByExtensionAndNumber(type, extension);
        if (fd != null) {
            responseObserver.onNext(createServerReflectionResponse(request, fd));
        } else {
            sendErrorResponse(request, TriRpcStatus.Code.NOT_FOUND, "Extension not found.",
                responseObserver);
        }
    }

    private void getAllExtensions(ServerReflectionRequest request,
        StreamObserver<ServerReflectionResponse> responseObserver) {
        String type = request.getAllExtensionNumbersOfType();
        Set<Integer> extensions = SchemaDescriptorRegistry.getExtensionNumbers(type);
        if (extensions != null) {
            ExtensionNumberResponse.Builder builder =
                ExtensionNumberResponse.newBuilder()
                    .setBaseTypeName(type)
                    .addAllExtensionNumber(extensions);
            responseObserver.onNext(
                ServerReflectionResponse.newBuilder()
                    .setValidHost(request.getHost())
                    .setOriginalRequest(request)
                    .setAllExtensionNumbersResponse(builder)
                    .build());
        } else {
            sendErrorResponse(request, TriRpcStatus.Code.NOT_FOUND, "Type not found.",
                responseObserver);
        }
    }

    private void listServices(ServerReflectionRequest request,
        StreamObserver<ServerReflectionResponse> responseObserver) {
        ListServiceResponse.Builder builder = ListServiceResponse.newBuilder();

        for (String serviceName : SchemaDescriptorRegistry.listServiceNames()) {
            builder.addService(ServiceResponse.newBuilder().setName(serviceName));
        }
        responseObserver.onNext(
            ServerReflectionResponse.newBuilder()
                .setValidHost(request.getHost())
                .setOriginalRequest(request)
                .setListServicesResponse(builder)
                .build());
    }

    private void sendErrorResponse(
        ServerReflectionRequest request, TriRpcStatus.Code code, String message,
        StreamObserver<ServerReflectionResponse> responseObserver) {
        ServerReflectionResponse response =
            ServerReflectionResponse.newBuilder()
                .setValidHost(request.getHost())
                .setOriginalRequest(request)
                .setErrorResponse(
                    ErrorResponse.newBuilder()
                        .setErrorCode(code.code)
                        .setErrorMessage(message))
                .build();
        responseObserver.onNext(response);
    }

    private ServerReflectionResponse createServerReflectionResponse(
        ServerReflectionRequest request, FileDescriptor fd) {
        FileDescriptorResponse.Builder fdRBuilder = FileDescriptorResponse.newBuilder();
        Set<String> seenFiles = new HashSet<>();
        Queue<FileDescriptor> frontier = new ArrayDeque<>();
        seenFiles.add(fd.getName());
        frontier.add(fd);
        while (!frontier.isEmpty()) {
            FileDescriptor nextFd = frontier.remove();
            fdRBuilder.addFileDescriptorProto(nextFd.toProto().toByteString());
            for (FileDescriptor dependencyFd : nextFd.getDependencies()) {
                if (!seenFiles.contains(dependencyFd.getName())) {
                    seenFiles.add(dependencyFd.getName());
                    frontier.add(dependencyFd);
                }
            }
        }
        return ServerReflectionResponse.newBuilder()
            .setValidHost(request.getHost())
            .setOriginalRequest(request)
            .setFileDescriptorResponse(fdRBuilder)
            .build();
    }
}
