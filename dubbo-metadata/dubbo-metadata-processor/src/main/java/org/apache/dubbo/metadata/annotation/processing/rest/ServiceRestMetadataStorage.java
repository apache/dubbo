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

import org.apache.dubbo.metadata.annotation.processing.ClassPathMetadataStorage;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

import com.google.gson.Gson;

import javax.annotation.processing.ProcessingEnvironment;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.google.gson.reflect.TypeToken.getParameterized;
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
        storage.read(SERVICE_REST_METADATA_RESOURCE_PATH, reader -> {
            Gson gson = new Gson();
            return (List) gson.fromJson(reader, getParameterized(List.class, ServiceRestMetadata.class).getType());
        }).ifPresent(existedMetadata -> {
            // Add all existed ServiceRestMetadata
            serviceRestMetadata.addAll(existedMetadata);
        });
        write(serviceRestMetadata);
    }

    public void write(Set<ServiceRestMetadata> serviceRestMetadata) throws IOException {
        if (serviceRestMetadata.isEmpty()) {
            return;
        }
        storage.write(() -> new Gson().toJson(serviceRestMetadata), SERVICE_REST_METADATA_RESOURCE_PATH);
    }

}
