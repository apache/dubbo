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

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.unmodifiableSortedSet;

public class MetadataServiceUtils {

    /**
     * Convert the specified {@link Iterable} of {@link URL URLs} to be the {@link URL#toFullString() strings} presenting
     * the {@link URL URLs}
     *
     * @param iterable {@link Iterable} of {@link URL}
     * @return the non-null read-only {@link SortedSet sorted set} of {@link URL#toFullString() strings} presenting
     * @see URL#toFullString()
     */
    public static SortedSet<String> toSortedStrings(Iterable<URL> iterable) {
        return toSortedStrings(StreamSupport.stream(iterable.spliterator(), false));
    }

    /**
     * Convert the specified {@link Stream} of {@link URL URLs} to be the {@link URL#toFullString() strings} presenting
     * the {@link URL URLs}
     *
     * @param stream {@link Stream} of {@link URL}
     * @return the non-null read-only {@link SortedSet sorted set} of {@link URL#toFullString() strings} presenting
     * @see URL#toFullString()
     */
    public static SortedSet<String> toSortedStrings(Stream<URL> stream) {
        return unmodifiableSortedSet(stream.map(URL::toFullString).collect(TreeSet::new, Set::add, Set::addAll));
    }

    static boolean isMetadataService(String interfaceName) {
        return interfaceName != null && interfaceName.equals(MetadataService.class.getName());
    }
}
