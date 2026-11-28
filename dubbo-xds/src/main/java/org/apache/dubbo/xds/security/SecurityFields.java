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
package org.apache.dubbo.xds.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

/** Security messages cannot silently drop restrictions the SDK does not implement. */
public final class SecurityFields {
    private SecurityFields() {}

    public static void only(Message message, String... names) {
        Set<String> supported = new HashSet<>(Arrays.asList(names));
        for (FieldDescriptor field : message.getAllFields().keySet()) {
            if (!supported.contains(field.getName())) {
                throw new IllegalArgumentException("Unsupported security field: "
                        + message.getDescriptorForType().getFullName() + "." + field.getName());
            }
        }
        if (!message.getUnknownFields().asMap().isEmpty()) {
            throw new IllegalArgumentException("Unknown security fields in "
                    + message.getDescriptorForType().getFullName());
        }
    }
}
