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

package org.apache.dubbo.errorcode.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities of iterating file.
 */
public final class FileUtils {
    private FileUtils() {
        throw new UnsupportedOperationException("No instance of FileUtils for you! ");
    }

    public static List<Path> getAllClassFilePaths(String rootPath) {
        List<Path> targetFolders;

        try (Stream<Path> filesStream = Files.walk(Paths.get(rootPath))) {
            targetFolders = filesStream.filter(x -> !x.toFile().isFile())
                    .filter(x -> x.toString().contains("classes") && !x.toString().contains("test-classes"))
                    .filter(x -> x.toString().contains("\\org\\apache\\dubbo".replace('\\', File.separatorChar)))
                    .collect(Collectors.toList());

            return targetFolders;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
