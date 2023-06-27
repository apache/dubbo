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
package org.apache.dubbo.aot.generate;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ResourceConfigMetadataRepository {

    private final List<ResourcePatternDescriber> includes;

    private final List<ResourcePatternDescriber> excludes;

    private final Set<ResourceBundleDescriber> resourceBundles;

    public ResourceConfigMetadataRepository() {
        this.includes = new ArrayList<>();
        this.excludes = new ArrayList<>();
        this.resourceBundles = new LinkedHashSet<>();
    }

    public ResourceConfigMetadataRepository registerIncludesPatterns(String... patterns) {
        for (String pattern : patterns) {
            registerIncludesPattern(new ResourcePatternDescriber(pattern,null));
        }
        return this;
    }

    public ResourceConfigMetadataRepository registerIncludesPattern(ResourcePatternDescriber describer) {
        this.includes.add(describer);
        return this;
    }

    public ResourceConfigMetadataRepository registerExcludesPattern(ResourcePatternDescriber describer) {
        this.excludes.add(describer);
        return this;
    }

    public ResourceConfigMetadataRepository registerBundles(ResourceBundleDescriber describer) {
        this.resourceBundles.add(describer);
        return this;
    }

    public List<ResourcePatternDescriber> getIncludes() {
        return includes;
    }

    public List<ResourcePatternDescriber> getExcludes() {
        return excludes;
    }

    public Set<ResourceBundleDescriber> getResourceBundles() {
        return resourceBundles;
    }
}
