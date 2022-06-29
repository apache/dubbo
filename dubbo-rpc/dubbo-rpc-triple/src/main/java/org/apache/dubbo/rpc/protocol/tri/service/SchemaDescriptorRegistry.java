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

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SchemaDescriptorRegistry {

    private static final Map<String, FileDescriptor> DESCRIPTORS_BY_SYMBOL = new ConcurrentHashMap<>();

    private static final Map<String, Map<Integer, FileDescriptor>> EXTENSIONS = new ConcurrentHashMap<>();

    private static final Set<String> SERVICES = new HashSet<>();

    public static void addSchemaDescriptor(String serviceName, FileDescriptor fd) {
        SERVICES.add(serviceName);
        DESCRIPTORS_BY_SYMBOL.put(serviceName, fd);
        for (Descriptor messageType : fd.getMessageTypes()) {
            addType(messageType);
        }
        for (FieldDescriptor extension : fd.getExtensions()) {
            addExtension(extension, fd);
        }
    }

    private static void addType(Descriptor descriptor) {
        DESCRIPTORS_BY_SYMBOL.put(descriptor.getFullName(), descriptor.getFile());
        for (Descriptor nestedType : descriptor.getNestedTypes()) {
            addType(nestedType);
        }
    }

    private static void addExtension(FieldDescriptor extension, FileDescriptor fd) {
        String name = extension.getContainingType().getFullName();
        int number = extension.getNumber();
        if (!EXTENSIONS.containsKey(name)) {
            EXTENSIONS.put(name, new HashMap<>());
        }
        Map<Integer, FileDescriptor> fdMap = EXTENSIONS.get(name);
        fdMap.put(number, fd);
    }

    public static FileDescriptor getFileDescriptorByExtensionAndNumber(String extension,
        int number) {
        return EXTENSIONS.getOrDefault(extension, Collections.emptyMap()).get(number);
    }

    public static Set<Integer> getExtensionNumbers(String extension) {
        Map<Integer, FileDescriptor> ret = EXTENSIONS.get(extension);
        if (ret == null) {
            return null;
        } else {
            return ret.keySet();
        }
    }

    public static FileDescriptor getSchemaDescriptor(String serviceName) {
        return DESCRIPTORS_BY_SYMBOL.get(serviceName);
    }

    public static List<String> listServiceNames() {
        return new ArrayList<>(SERVICES);
    }
}
