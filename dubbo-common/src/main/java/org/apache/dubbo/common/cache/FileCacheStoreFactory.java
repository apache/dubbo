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

package org.apache.dubbo.common.cache;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_CACHE_PATH_INACCESSIBLE;

/**
 * ClassLoader Level static share.
 * Prevent FileCacheStore being operated in multi-application
 */
public final class FileCacheStoreFactory {

    /**
     * Forbids instantiation.
     */
    private FileCacheStoreFactory() {
        throw new UnsupportedOperationException("No instance of 'FileCacheStoreFactory' for you! ");
    }

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(FileCacheStoreFactory.class);
    private static final ConcurrentMap<String, FileCacheStore> cacheMap = new ConcurrentHashMap<>();

    private static final String SUFFIX = ".dubbo.cache";
    private static final char ESCAPE_MARK = '%';
    private static final Set<Character> LEGAL_CHARACTERS = Collections.unmodifiableSet(new HashSet<Character>() {{
        // - $ . _ 0-9 a-z A-Z
        add('-');
        add('$');
        add('.');
        add('_');
        for (char c = '0'; c <= '9'; c++) {
            add(c);
        }
        for (char c = 'a'; c <= 'z'; c++) {
            add(c);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            add(c);
        }
    }});

    public static FileCacheStore getInstance(String basePath, String cacheName) {
        return getInstance(basePath, cacheName, true);
    }

    public static FileCacheStore getInstance(String basePath, String cacheName, boolean enableFileCache) {
        if (basePath == null) {
            // default case: ~/.dubbo
            basePath = System.getProperty("user.home") + File.separator + ".dubbo";
        }
        if (basePath.endsWith(File.separator)) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        File candidate = new File(basePath);
        Path path = candidate.toPath();

        // ensure cache store path exists
        if (!candidate.isDirectory()) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                // 0-3 - cache path inaccessible

                logger.error(COMMON_CACHE_PATH_INACCESSIBLE, "inaccessible of cache path", "",
                    "Cache store path can't be created: ", e);

                throw new RuntimeException("Cache store path can't be created: " + candidate, e);
            }
        }

        cacheName = safeName(cacheName);
        if (!cacheName.endsWith(SUFFIX)) {
            cacheName = cacheName + SUFFIX;
        }

        String cacheFilePath = basePath + File.separator + cacheName;

        return ConcurrentHashMapUtils.computeIfAbsent(cacheMap, cacheFilePath, k -> getFile(k, enableFileCache));
    }

    /**
     * sanitize a name for valid file or directory name
     *
     * @param name origin file name
     * @return sanitized version of name
     */
    private static String safeName(String name) {
        int len = name.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = name.charAt(i);
            if (LEGAL_CHARACTERS.contains(c)) {
                sb.append(c);
            } else {
                sb.append(ESCAPE_MARK);
                sb.append(String.format("%04x", (int) c));
            }
        }
        return sb.toString();
    }

    /**
     * Get a file object for the given name
     *
     * @param name the file name
     * @return a file object
     */
    private static FileCacheStore getFile(String name, boolean enableFileCache) {
        if (!enableFileCache) {
            return FileCacheStore.Empty.getInstance(name);
        }

        try {
            FileCacheStore.Builder builder = FileCacheStore.newBuilder();
            tryFileLock(builder, name);
            File file = new File(name);

            if (!file.exists()) {
                Path pathObjectOfFile = file.toPath();
                Files.createFile(pathObjectOfFile);
            }

            builder.cacheFilePath(name)
                .cacheFile(file);

            return builder.build();
        } catch (Throwable t) {

            logger.warn(COMMON_CACHE_PATH_INACCESSIBLE, "inaccessible of cache path", "",
                "Failed to create file store cache. Local file cache will be disabled. Cache file name: " + name, t);

            return FileCacheStore.Empty.getInstance(name);
        }
    }

    private static void tryFileLock(FileCacheStore.Builder builder, String fileName) throws PathNotExclusiveException {
        File lockFile = new File(fileName + ".lock");

        FileLock dirLock;
        try {
            lockFile.createNewFile();
            if (!lockFile.exists()) {
                throw new AssertionError("Failed to create lock file " + lockFile);
            }
            FileChannel lockFileChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            dirLock = lockFileChannel.tryLock();
        } catch (OverlappingFileLockException ofle) {
            dirLock = null;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        if (dirLock == null) {
            throw new PathNotExclusiveException(fileName + " is not exclusive. Maybe multiple Dubbo instances are using the same folder.");
        }

        lockFile.deleteOnExit();
        builder.directoryLock(dirLock).lockFile(lockFile);
    }

    static void removeCache(String cacheFileName) {
        cacheMap.remove(cacheFileName);
    }

    /**
     * for unit test only
     */
    @Deprecated
    static Map<String, FileCacheStore> getCacheMap() {
        return cacheMap;
    }

    private static class PathNotExclusiveException extends Exception {
        public PathNotExclusiveException(String msg) {
            super(msg);
        }
    }

}
