/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package org.apache.dubbo.gen.utils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos;

import javax.annotation.Nonnull;
import java.util.Collection;

public final class ProtoTypeMap {
    private static final Joiner DOT_JOINER = Joiner.on('.').skipNulls();
    private final ImmutableMap<String, String> types;

    private ProtoTypeMap(@Nonnull ImmutableMap<String, String> types) {
        Preconditions.checkNotNull(types, "types");
        this.types = types;
    }

    public static ProtoTypeMap of(@Nonnull Collection<DescriptorProtos.FileDescriptorProto> fileDescriptorProtos) {
        Preconditions.checkNotNull(fileDescriptorProtos, "fileDescriptorProtos");
        Preconditions.checkArgument(!fileDescriptorProtos.isEmpty(), "fileDescriptorProtos.isEmpty()");
        ImmutableMap.Builder<String, String> types = ImmutableMap.builder();

        for (DescriptorProtos.FileDescriptorProto fileDescriptor : fileDescriptorProtos) {
            DescriptorProtos.FileOptions fileOptions = fileDescriptor.getOptions();
            String protoPackage = fileDescriptor.hasPackage() ? "." + fileDescriptor.getPackage() : "";
            String javaPackage = Strings.emptyToNull(fileOptions.hasJavaPackage() ? fileOptions.getJavaPackage() : fileDescriptor.getPackage());
            String enclosingClassName = fileOptions.getJavaMultipleFiles() ? null : getJavaOuterClassname(fileDescriptor, fileOptions);
            fileDescriptor.getEnumTypeList().forEach((e) -> {
                types.put(protoPackage + "." + e.getName(), DOT_JOINER.join(javaPackage, enclosingClassName, new Object[]{e.getName()}));
            });
            fileDescriptor.getMessageTypeList().forEach((m) -> {
                recursivelyAddTypes(types, m, protoPackage, enclosingClassName, javaPackage);
            });
        }

        return new ProtoTypeMap(types.build());
    }

    private static void recursivelyAddTypes(ImmutableMap.Builder<String, String> types, DescriptorProtos.DescriptorProto m, String protoPackage, String enclosingClassName, String javaPackage) {
        String protoTypeName = protoPackage + "." + m.getName();
        types.put(protoTypeName, DOT_JOINER.join(javaPackage, enclosingClassName, new Object[]{m.getName()}));
        m.getEnumTypeList().forEach((e) -> {
            types.put(protoPackage + "." + m.getName() + "." + e.getName(), DOT_JOINER.join(javaPackage, enclosingClassName, new Object[]{m.getName(), e.getName()}));
        });
        m.getNestedTypeList().forEach((n) -> {
            recursivelyAddTypes(types, n, protoPackage + "." + m.getName(), DOT_JOINER.join(enclosingClassName, m.getName(), new Object[0]), javaPackage);
        });
    }

    public String toJavaTypeName(@Nonnull String protoTypeName) {
        Preconditions.checkNotNull(protoTypeName, "protoTypeName");
        return (String)this.types.get(protoTypeName);
    }

    public static String getJavaOuterClassname(DescriptorProtos.FileDescriptorProto fileDescriptor, DescriptorProtos.FileOptions fileOptions) {
        if (fileOptions.hasJavaOuterClassname()) {
            return fileOptions.getJavaOuterClassname();
        } else {
            String filename = fileDescriptor.getName().substring(0, fileDescriptor.getName().length() - ".proto".length());
            if (filename.contains("/")) {
                filename = filename.substring(filename.lastIndexOf(47) + 1);
            }

            filename = makeInvalidCharactersUnderscores(filename);
            filename = convertToCamelCase(filename);
            filename = appendOuterClassSuffix(filename, fileDescriptor);
            return filename;
        }
    }

    private static String appendOuterClassSuffix(String enclosingClassName, DescriptorProtos.FileDescriptorProto fd) {
        return !fd.getEnumTypeList().stream().anyMatch((enumProto) -> {
            return enumProto.getName().equals(enclosingClassName);
        }) && !fd.getMessageTypeList().stream().anyMatch((messageProto) -> {
            return messageProto.getName().equals(enclosingClassName);
        }) && !fd.getServiceList().stream().anyMatch((serviceProto) -> {
            return serviceProto.getName().equals(enclosingClassName);
        }) ? enclosingClassName : enclosingClassName + "OuterClass";
    }

    private static String makeInvalidCharactersUnderscores(String filename) {
        char[] filechars = filename.toCharArray();

        for(int i = 0; i < filechars.length; ++i) {
            char c = filechars[i];
            if (!CharMatcher.inRange('0', '9').or(CharMatcher.inRange('A', 'Z')).or(CharMatcher.inRange('a', 'z')).matches(c)) {
                filechars[i] = '_';
            }
        }

        return new String(filechars);
    }

    private static String convertToCamelCase(String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(name.charAt(0)));

        for(int i = 1; i < name.length(); ++i) {
            char c = name.charAt(i);
            char prev = name.charAt(i - 1);
            if (c != '_') {
                if (prev != '_' && !CharMatcher.inRange('0', '9').matches(prev)) {
                    sb.append(c);
                } else {
                    sb.append(Character.toUpperCase(c));
                }
            }
        }

        return sb.toString();
    }
}

