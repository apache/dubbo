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
package org.apache.dubbo.config.spring.beans.factory.config;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 * YAML {@link PropertySourceFactory} implementation, some source code is copied Spring Boot
 * org.springframework.boot.env.YamlPropertySourceLoader , see {@link #createYaml()} and {@link #process()}
 *
 * @since 2.6.5
 */
public class YamlPropertySourceFactory extends YamlProcessor implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        setResources(resource.getResource());
        return new MapPropertySource(name, process());
    }

    @Override
    protected Yaml createYaml() {
        return new Yaml(new Constructor() {
            @Override
            protected Map<Object, Object> constructMapping(MappingNode node) {
                try {
                    return super.constructMapping(node);
                } catch (IllegalStateException ex) {
                    throw new ParserException("while parsing MappingNode",
                            node.getStartMark(), ex.getMessage(), node.getEndMark());
                }
            }

            @Override
            protected Map<Object, Object> createDefaultMap() {
                final Map<Object, Object> delegate = super.createDefaultMap();
                return new AbstractMap<Object, Object>() {
                    @Override
                    public Object put(Object key, Object value) {
                        if (delegate.containsKey(key)) {
                            throw new IllegalStateException("Duplicate key: " + key);
                        }
                        return delegate.put(key, value);
                    }

                    @Override
                    public Set<Entry<Object, Object>> entrySet() {
                        return delegate.entrySet();
                    }
                };
            }
        }, new Representer(),
                new DumperOptions(), new Resolver() {
            @Override
            public void addImplicitResolver(Tag tag, Pattern regexp,
                                            String first) {
                if (tag == Tag.TIMESTAMP) {
                    return;
                }
                super.addImplicitResolver(tag, regexp, first);
            }
        });
    }

    /**
     * {@link Resolver} that limits {@link Tag#TIMESTAMP} tags.
     */
    private static class LimitedResolver extends Resolver {
        @Override
        public void addImplicitResolver(Tag tag, Pattern regexp, String first) {
            if (tag == Tag.TIMESTAMP) {
                return;
            }
            super.addImplicitResolver(tag, regexp, first);
        }
    }

    public Map<String, Object> process() {
        final Map<String, Object> result = new LinkedHashMap<String, Object>();
        process((properties, map) -> result.putAll(getFlattenedMap(map)));
        return result;
    }
}
