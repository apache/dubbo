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
import org.apache.dubbo.common.compiler.support.ClassUtils;
import org.apache.dubbo.metadata.MetadataService;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;

/**
 * A class to resolve the version from {@link URL URLs}
 *
 * @since 2.7.5
 */
public class URLRevisionResolver {

    public static final String NO_REVISION = "N/A";

    /**
     * Resolve revision as {@link String}
     *
     * @param urls {@link URL#toFullString() strings} presenting the {@link URL URLs}
     * @return non-null
     */
    public String resolve(Collection<String> urls) {

        if (isEmpty(urls)) {
            return NO_REVISION;
        }

        List<URL> urlsList = toURLsList(urls);

        SortedSet<String> methodSignatures = resolveMethodSignatures(urlsList);

        SortedSet<String> urlParameters = resolveURLParameters(urlsList);

        SortedSet<String> values = new TreeSet<>(methodSignatures);

        values.addAll(urlParameters);

        return values.stream()
                .map(this::hashCode)                     // generate Long hashCode
                .reduce(Long::sum)                       // sum hashCode
                .map(String::valueOf)                    // Long to String
                .orElse(NO_REVISION);                    // NO_REVISION as default
    }

    private List<URL> toURLsList(Collection<String> urls) {
        return urls.stream()
                .map(URL::valueOf)                             // String to URL
                .filter(url -> isNotMetadataService(url.getServiceInterface())) // filter not MetadataService interface
                .collect(Collectors.toList());
    }

    private SortedSet<String> resolveMethodSignatures(List<URL> urls) {
        return urls.stream()
                .map(URL::getServiceInterface)                 // get the service interface
                .map(ClassUtils::forName)                      // load business interface class
                .map(Class::getMethods)                        // get all public methods from business interface
                .map(Arrays::asList)                           // Array to List
                .flatMap(Collection::stream)                   // flat Stream<Stream> to be Stream
                .map(Object::toString)                         // Method to String
                .collect(TreeSet::new, Set::add, Set::addAll); // sort and remove the duplicate
    }

    private SortedSet<String> resolveURLParameters(Collection<URL> urls) {
        return urls.stream()
                .map(url -> url.removeParameter(PID_KEY))
                .map(url -> url.removeParameter(TIMESTAMP_KEY))
                .map(URL::toParameterString)
                .collect(TreeSet::new, Set::add, Set::addAll); // sort and remove the duplicate
    }

    private long hashCode(String value) {
        long h = 0;
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            h = 31L * h + chars[i];
        }
        return h;
    }

    private boolean isNotMetadataService(String serviceInterface) {
        return !MetadataService.class.getName().equals(serviceInterface);
    }
}
