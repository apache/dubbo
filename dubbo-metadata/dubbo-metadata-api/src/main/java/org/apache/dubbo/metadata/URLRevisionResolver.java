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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.compiler.support.ClassUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;

/**
 * A class to resolve the version from {@link URL URLs}
 *
 * @revised 2.7.8 repackage and refactor
 * @since 2.7.5
 */
public class URLRevisionResolver {

    /**
     * @since 2.7.8
     */
    public static final String UNKNOWN_REVISION = "X";

    /**
     * @since 2.7.8
     */
    public static final URLRevisionResolver INSTANCE = new URLRevisionResolver();

    /**
     * Resolve revision as {@link String} from the specified the {@link URL#toFullString() strings} presenting the {@link URL URLs}.
     *
     * @param url    one {@link URL}
     * @param others the others {@link URL}
     * @return non-null
     * @since 2.7.8
     */
    public String resolve(String url, String... others) {
        List<String> urls = new ArrayList<>(others.length + 1);
        urls.add(url);
        urls.addAll(Arrays.asList(others));
        return resolve(urls);
    }

    /**
     * Resolve revision as {@link String}
     *
     * @param urls {@link URL#toFullString() strings} presenting the {@link URL URLs}
     * @return non-null
     * @revised 2.7.8 refactor the parameter as the super interface (from Collection to Iterable)
     */
    public String resolve(Iterable<String> urls) {
        List<URL> urlsList = toURLsList(urls);
        return resolve(urlsList);
    }

    /**
     * Resolve revision as {@link String} from the specified the {@link URL URLs}.
     *
     * @param urls the {@link URL URLs}
     * @return non-null
     * @since 2.7.8
     */
    public String resolve(Collection<URL> urls) {

        if (isEmpty(urls)) {
            return UNKNOWN_REVISION;
        }

        SortedSet<String> methodSignatures = resolveMethodSignatures(urls);

        SortedSet<String> urlParameters = resolveURLParameters(urls);

        SortedSet<String> values = new TreeSet<>(methodSignatures);

        values.addAll(urlParameters);

        return values.stream()
                .map(this::hashCode)                     // generate Long hashCode
                .reduce(Long::sum)                       // sum hashCode
                .map(Long::toHexString)                  // Using Hex for the shorten content
                .orElse(UNKNOWN_REVISION);               // NO_REVISION as default
    }

    private List<URL> toURLsList(Iterable<String> urls) {
        if (urls == null) {
            return emptyList();
        }
        return StreamSupport.
                stream(urls.spliterator(), false)
                .map(URL::valueOf)                             // String to URL
                .filter(url -> isNotMetadataService(url.getServiceInterface())) // filter not MetadataService interface
                .collect(Collectors.toList());
    }

    private SortedSet<String> resolveMethodSignatures(Collection<URL> urls) {
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
