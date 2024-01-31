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
package org.apache.dubbo.metadata.rest;

import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.common.utils.JsonUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static org.apache.dubbo.common.function.ThrowableAction.execute;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.METADATA_ENCODING;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.SERVICE_REST_METADATA_RESOURCE_PATH;

/**
 * Class-Path based {@link ServiceRestMetadataReader} implementation
 *
 * @see ServiceRestMetadataReader
 * @since 2.7.6
 */
public class ClassPathServiceRestMetadataReader implements ServiceRestMetadataReader {

    private final String serviceRestMetadataJsonResourcePath;

    public ClassPathServiceRestMetadataReader() {
        this(SERVICE_REST_METADATA_RESOURCE_PATH);
    }

    public ClassPathServiceRestMetadataReader(String serviceRestMetadataJsonResourcePath) {
        this.serviceRestMetadataJsonResourcePath = serviceRestMetadataJsonResourcePath;
    }

    @Override
    public List<ServiceRestMetadata> read() {

        List<ServiceRestMetadata> serviceRestMetadataList = new LinkedList<>();

        ClassLoader classLoader = getClass().getClassLoader();

        execute(() -> {
            Enumeration<URL> resources = classLoader.getResources(serviceRestMetadataJsonResourcePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                InputStream inputStream = resource.openStream();
                String json = IOUtils.read(inputStream, METADATA_ENCODING);
                serviceRestMetadataList.addAll(JsonUtils.toJavaList(json, ServiceRestMetadata.class));
            }
        });

        return unmodifiableList(serviceRestMetadataList);
    }
}
