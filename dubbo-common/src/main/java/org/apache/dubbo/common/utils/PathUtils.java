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
package org.apache.dubbo.common.utils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.apache.dubbo.common.utils.StringUtils.QUESTION_MASK;
import static org.apache.dubbo.common.utils.StringUtils.SLASH;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.replace;

/**
 * Path Utilities class
 *
 * @since 2.7.6
 */
public interface PathUtils {

    static String buildPath(String rootPath, String... subPaths) {

        Set<String> paths = new LinkedHashSet<>();
        paths.add(rootPath);
        paths.addAll(asList(subPaths));

        return normalize(paths.stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(SLASH)));
    }

    /**
     * Normalize path:
     * <ol>
     * <li>To remove query string if presents</li>
     * <li>To remove duplicated slash("/") if exists</li>
     * </ol>
     *
     * @param path path to be normalized
     * @return a normalized path if required
     */
    static String normalize(String path) {
        if (isEmpty(path)) {
            return SLASH;
        }
        String normalizedPath = path;
        int index = normalizedPath.indexOf(QUESTION_MASK);
        if (index > -1) {
            normalizedPath = normalizedPath.substring(0, index);
        }

        while (normalizedPath.contains("//")) {
            normalizedPath = replace(normalizedPath, "//", "/");
        }
        return normalizedPath;
    }

}
