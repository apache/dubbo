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

import org.apache.dubbo.metadata.annotation.processing.ClassPathMetadataWriter;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

import com.google.gson.Gson;

import javax.annotation.processing.ProcessingEnvironment;
import java.io.IOException;
import java.util.Set;

/**
 * The writer for {@link ServiceRestMetadata}
 */
public class ServiceRestMetadataWriter {

    public static final String METADATA_RESOURCE_PATH = "META-INF/dubbo/service-rest-metadata.json";

    private final ClassPathMetadataWriter writer;

    public ServiceRestMetadataWriter(ProcessingEnvironment processingEnv) {
        this.writer = new ClassPathMetadataWriter(processingEnv);
    }

    public void write(Set<ServiceRestMetadata> serviceRestMetadata) throws IOException {
        if (serviceRestMetadata.isEmpty()) {
            return;
        }
        Gson gson = new Gson();
        String json = gson.toJson(serviceRestMetadata);
        writer.write(json, METADATA_RESOURCE_PATH);
    }

}
