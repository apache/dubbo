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
package org.apache.dubbo.gen;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.Feature;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractGenerator extends Generator {

    private static final int SERVICE_NUMBER_OF_PATHS = 2;
    private static final int METHOD_NUMBER_OF_PATHS = 4;

    protected abstract String getClassPrefix();

    protected abstract String getClassSuffix();

    protected String getSingleTemplateFileName() {
        return getTemplateFileName();
    }

    protected String getTemplateFileName() {
        return getClassPrefix() + getClassSuffix() + "Stub.mustache";
    }

    protected String getInterfaceTemplateFileName() {
        return getClassPrefix() + getClassSuffix() + "InterfaceStub.mustache";
    }

    @Override
    protected List<Feature> supportedFeatures() {
        return Collections.singletonList(Feature.FEATURE_PROTO3_OPTIONAL);
    }

    private String getServiceJavaDocPrefix() {
        return "    ";
    }

    private String getMethodJavaDocPrefix() {
        return "        ";
    }

    @Override
    public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(
        PluginProtos.CodeGeneratorRequest request) throws GeneratorException {
        final ProtoTypeMap typeMap = ProtoTypeMap.of(request.getProtoFileList());

        List<FileDescriptorProto> protosToGenerate = request.getProtoFileList().stream()
            .filter(protoFile -> request.getFileToGenerateList().contains(protoFile.getName()))
            .collect(Collectors.toList());

        List<ServiceContext> services = findServices(protosToGenerate, typeMap);
        return generateFiles(services);
    }

    private List<ServiceContext> findServices(List<FileDescriptorProto> protos,
        ProtoTypeMap typeMap) {
        List<ServiceContext> contexts = new ArrayList<>();

        protos.forEach(fileProto -> {
            for (int serviceNumber = 0; serviceNumber < fileProto.getServiceCount();
                serviceNumber++) {
                ServiceContext serviceContext = buildServiceContext(
                    fileProto.getService(serviceNumber),
                    typeMap,
                    fileProto.getSourceCodeInfo().getLocationList(),
                    serviceNumber
                );
                serviceContext.protoName = fileProto.getName();
                serviceContext.packageName = extractPackageName(fileProto);
                if (!Strings.isNullOrEmpty(fileProto.getOptions().getJavaOuterClassname())) {
                    serviceContext.outerClassName = fileProto.getOptions().getJavaOuterClassname();
                }
                serviceContext.commonPackageName = extractCommonPackageName(fileProto);
                serviceContext.multipleFiles =
                    fileProto.getOptions().getJavaMultipleFiles();
                contexts.add(serviceContext);
            }
        });

        return contexts;
    }

    private String extractPackageName(FileDescriptorProto proto) {
        FileOptions options = proto.getOptions();
        String javaPackage = options.getJavaPackage();
        if (!Strings.isNullOrEmpty(javaPackage)) {
            return javaPackage;
        }

        return Strings.nullToEmpty(proto.getPackage());
    }

    private String extractCommonPackageName(FileDescriptorProto proto) {
        return Strings.nullToEmpty(proto.getPackage());
    }

    private ServiceContext buildServiceContext(ServiceDescriptorProto serviceProto,
        ProtoTypeMap typeMap, List<Location> locations, int serviceNumber) {
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.fileName =
            getClassPrefix() + serviceProto.getName() + getClassSuffix() + ".java";
        serviceContext.className = getClassPrefix() + serviceProto.getName() + getClassSuffix();
        serviceContext.outerClassName = serviceProto.getName() + "OuterClass";
        serviceContext.interfaceFileName = serviceProto.getName() + ".java";
        serviceContext.interfaceClassName = serviceProto.getName();
        serviceContext.serviceName = serviceProto.getName();
        serviceContext.deprecated =
            serviceProto.getOptions().getDeprecated();

        List<Location> allLocationsForService = locations.stream()
            .filter(location ->
                location.getPathCount() >= 2 &&
                    location.getPath(0) == FileDescriptorProto.SERVICE_FIELD_NUMBER &&
                    location.getPath(1) == serviceNumber
            )
            .collect(Collectors.toList());

        Location serviceLocation = allLocationsForService.stream()
            .filter(location -> location.getPathCount() == SERVICE_NUMBER_OF_PATHS)
            .findFirst()
            .orElseGet(Location::getDefaultInstance);
        serviceContext.javaDoc = getJavaDoc(getComments(serviceLocation),
            getServiceJavaDocPrefix());

        for (int methodNumber = 0; methodNumber < serviceProto.getMethodCount(); methodNumber++) {
            MethodContext methodContext = buildMethodContext(
                serviceProto.getMethod(methodNumber),
                typeMap,
                locations,
                methodNumber
            );

            serviceContext.methods.add(methodContext);
            serviceContext.methodTypes.add(methodContext.inputType);
            serviceContext.methodTypes.add(methodContext.outputType);
        }
        return serviceContext;
    }

    private MethodContext buildMethodContext(MethodDescriptorProto methodProto,
        ProtoTypeMap typeMap, List<Location> locations, int methodNumber) {
        MethodContext methodContext = new MethodContext();
        methodContext.originMethodName = methodProto.getName();
        methodContext.methodName = lowerCaseFirst(methodProto.getName());
        methodContext.inputType = typeMap.toJavaTypeName(methodProto.getInputType());
        methodContext.outputType = typeMap.toJavaTypeName(methodProto.getOutputType());
        methodContext.deprecated =
            methodProto.getOptions().getDeprecated();
        methodContext.isManyInput = methodProto.getClientStreaming();
        methodContext.isManyOutput = methodProto.getServerStreaming();
        methodContext.methodNumber = methodNumber;

        Location methodLocation = locations.stream()
            .filter(location ->
                location.getPathCount() == METHOD_NUMBER_OF_PATHS &&
                    location.getPath(METHOD_NUMBER_OF_PATHS - 1) == methodNumber
            )
            .findFirst()
            .orElseGet(Location::getDefaultInstance);
        methodContext.javaDoc = getJavaDoc(getComments(methodLocation), getMethodJavaDocPrefix());

        if (!methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
            methodContext.reactiveCallsMethodName = "oneToOne";
            methodContext.grpcCallsMethodName = "asyncUnaryCall";
        }
        if (!methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
            methodContext.reactiveCallsMethodName = "oneToMany";
            methodContext.grpcCallsMethodName = "asyncServerStreamingCall";
        }
        if (methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
            methodContext.reactiveCallsMethodName = "manyToOne";
            methodContext.grpcCallsMethodName = "asyncClientStreamingCall";
        }
        if (methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
            methodContext.reactiveCallsMethodName = "manyToMany";
            methodContext.grpcCallsMethodName = "asyncBidiStreamingCall";
        }
        return methodContext;
    }

    private String lowerCaseFirst(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private List<PluginProtos.CodeGeneratorResponse.File> generateFiles(
        List<ServiceContext> services) {
        List<PluginProtos.CodeGeneratorResponse.File> allServiceFiles = new ArrayList<>();
        for (ServiceContext context : services) {
            List<PluginProtos.CodeGeneratorResponse.File> files = buildFile(context);
            allServiceFiles.addAll(files);
        }
        return allServiceFiles;
    }

    protected boolean enableMultipleTemplateFiles() {
        return false;
    }

    private List<PluginProtos.CodeGeneratorResponse.File> buildFile(ServiceContext context) {
        List<PluginProtos.CodeGeneratorResponse.File> files = new ArrayList<>();

        if (context.multipleFiles && enableMultipleTemplateFiles()) {
            String content = applyTemplate(getTemplateFileName(), context);
            String dir = absoluteDir(context);

            files.add(PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(getFileName(dir, context.fileName))
                .setContent(content)
                .build());

            content = applyTemplate(getInterfaceTemplateFileName(), context);
            files.add(PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(getFileName(dir, context.interfaceFileName))
                .setContent(content)
                .build());
        } else {
            String content = applyTemplate(getSingleTemplateFileName(), context);
            String dir = absoluteDir(context);

            files.add(PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(getFileName(dir, context.fileName))
                .setContent(content)
                .build());
        }

        return files;
    }

    private String absoluteDir(ServiceContext ctx) {
        return ctx.packageName.replace('.', '/');
    }

    private String getFileName(String dir, String fileName) {
        if (Strings.isNullOrEmpty(dir)) {
            return fileName;
        }
        return dir + "/" + fileName;
    }

    private String getComments(Location location) {
        return location.getLeadingComments().isEmpty() ? location.getTrailingComments()
            : location.getLeadingComments();
    }

    private String getJavaDoc(String comments, String prefix) {
        if (!comments.isEmpty()) {
            StringBuilder builder = new StringBuilder("/**\n")
                .append(prefix).append(" * <pre>\n");
            Arrays.stream(HtmlEscapers.htmlEscaper().escape(comments).split("\n"))
                .map(line -> line.replace("*/", "&#42;&#47;").replace("*", "&#42;"))
                .forEach(line -> builder.append(prefix).append(" * ").append(line).append("\n"));
            builder
                .append(prefix).append(" * </pre>\n")
                .append(prefix).append(" */");
            return builder.toString();
        }
        return null;
    }

    /**
     * Template class for proto Service objects.
     */
    private class ServiceContext {

        // CHECKSTYLE DISABLE VisibilityModifier FOR 8 LINES
        public String fileName;
        public String interfaceFileName;
        public String protoName;
        public String packageName;
        public String commonPackageName;
        public String className;
        public String interfaceClassName;
        public String serviceName;
        public boolean deprecated;
        public String javaDoc;
        public boolean multipleFiles;

        public String outerClassName;
        public List<MethodContext> methods = new ArrayList<>();

        public Set<String> methodTypes = new HashSet<>();

        public List<MethodContext> unaryRequestMethods() {
            return methods.stream().filter(m -> !m.isManyInput).collect(Collectors.toList());
        }

        public List<MethodContext> unaryMethods() {
            return methods.stream().filter(m -> (!m.isManyInput && !m.isManyOutput))
                .collect(Collectors.toList());
        }

        public List<MethodContext> serverStreamingMethods() {
            return methods.stream().filter(m -> !m.isManyInput && m.isManyOutput)
                .collect(Collectors.toList());
        }

        public List<MethodContext> biStreamingMethods() {
            return methods.stream().filter(m -> m.isManyInput).collect(Collectors.toList());
        }

        public List<MethodContext> biStreamingWithoutClientStreamMethods() {
            return methods.stream().filter(m -> m.isManyInput && m.isManyOutput)
                .collect(Collectors.toList());
        }

        public List<MethodContext> clientStreamingMethods() {
            return methods.stream().filter(m -> m.isManyInput && !m.isManyOutput)
                .collect(Collectors.toList());
        }

        public List<MethodContext> methods() {
            return methods;
        }

    }

    /**
     * Template class for proto RPC objects.
     */
    private static class MethodContext {

        // CHECKSTYLE DISABLE VisibilityModifier FOR 10 LINES
        public String originMethodName;
        public String methodName;
        public String inputType;
        public String outputType;
        public boolean deprecated;
        public boolean isManyInput;
        public boolean isManyOutput;
        public String reactiveCallsMethodName;
        public String grpcCallsMethodName;
        public int methodNumber;
        public String javaDoc;

        // This method mimics the upper-casing method ogf gRPC to ensure compatibility
        // See https://github.com/grpc/grpc-java/blob/v1.8.0/compiler/src/java_plugin/cpp/java_generator.cpp#L58
        public String methodNameUpperUnderscore() {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < methodName.length(); i++) {
                char c = methodName.charAt(i);
                s.append(Character.toUpperCase(c));
                if ((i < methodName.length() - 1) && Character.isLowerCase(c)
                    && Character.isUpperCase(methodName.charAt(i + 1))) {
                    s.append('_');
                }
            }
            return s.toString();
        }

        public String methodNamePascalCase() {
            String mn = methodName.replace("_", "");
            return String.valueOf(Character.toUpperCase(mn.charAt(0))) + mn.substring(1);
        }

        public String methodNameCamelCase() {
            String mn = methodName.replace("_", "");
            return String.valueOf(Character.toLowerCase(mn.charAt(0))) + mn.substring(1);
        }
    }
}
