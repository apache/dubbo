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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.ServiceLoader.load;
import static org.apache.dubbo.common.utils.CollectionUtils.isNotEmpty;

/**
 * The implementation of {@link MetadataServiceURLBuilder} composites the multiple {@link MetadataServiceURLBuilder}
 * instances are loaded by Java standard {@link ServiceLoader} will aggregate {@link URL URLs} for
 * {@link MetadataServiceProxy}
 *
 * @see MetadataServiceURLBuilder
 * @see MetadataServiceProxy
 * @see MetadataService
 * @see URL
 * @see ServiceLoader
 * @since 2.7.4
 */
class CompositeMetadataServiceURLBuilder implements MetadataServiceURLBuilder {

    private final Class<MetadataServiceURLBuilder> builderClass;

    private final Iterator<MetadataServiceURLBuilder> builders;

    private final ClassLoader classLoader;

    public CompositeMetadataServiceURLBuilder() {
        this.builderClass = MetadataServiceURLBuilder.class;
        this.classLoader = getClass().getClassLoader();
        this.builders = initBuilders();
    }

    private Iterator<MetadataServiceURLBuilder> initBuilders() {
        return load(builderClass, classLoader).iterator();
    }

    @Override
    public List<URL> build(ServiceInstance serviceInstance) {
        if (serviceInstance == null) {
            return emptyList();
        }

        List<URL> allURLs = new LinkedList<>();

        while (builders.hasNext()) {
            MetadataServiceURLBuilder builder = builders.next();
            List<URL> urls = builder.build(serviceInstance);
            if (isNotEmpty(urls)) {
                allURLs.addAll(urls);
            }
        }

        return unmodifiableList(allURLs);
    }
}
