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
package org.apache.dubbo.metadata.annotation.processing.rest;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.annotation.processing.ClassPathMetadataStorage;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

import javax.annotation.processing.ProcessingEnvironment;
import java.io.IOException;
import java.util.Set;

import static org.apache.dubbo.metadata.rest.RestMetadataConstants.SERVICE_REST_METADATA_RESOURCE_PATH;

/**
 * The storage for {@link ServiceRestMetadata}
 */
public class ServiceRestMetadataStorage {

    private final ClassPathMetadataStorage storage;

    public ServiceRestMetadataStorage(ProcessingEnvironment processingEnv) {
        this.storage = new ClassPathMetadataStorage(processingEnv);
    }

    public void append(Set<ServiceRestMetadata> serviceRestMetadata) throws IOException {
        // Add all existed ServiceRestMetadata
        storage.read(SERVICE_REST_METADATA_RESOURCE_PATH, reader -> {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                char[] buf = new char[1024];
                int len;
                while ((len = reader.read(buf)) != -1) {
                    stringBuilder.append(buf, 0, len);
                }
                return JsonUtils.toJavaList(stringBuilder.toString(), ServiceRestMetadata.class);
            } catch (IOException e) {
                return null;
            }
        }).ifPresent(serviceRestMetadata::addAll);
        write(serviceRestMetadata);
    }

    public void write(Set<ServiceRestMetadata> serviceRestMetadata) throws IOException {
        if (serviceRestMetadata.isEmpty()) {
            return;
        }
        storage.write(() -> JsonUtils.toJson(serviceRestMetadata), SERVICE_REST_METADATA_RESOURCE_PATH);
    }

}
