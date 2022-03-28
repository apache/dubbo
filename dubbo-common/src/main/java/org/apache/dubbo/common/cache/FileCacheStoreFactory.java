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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassLoader Level static share.
 * Prevent FileCacheStore being operated in multi-application
 */
public class FileCacheStoreFactory {
    private final static Logger logger = LoggerFactory.getLogger(FileCacheStoreFactory.class);
    private static final Map<String, FileCacheStore> cacheMap = new ConcurrentHashMap<>();

    private static final String SUFFIX = ".dubbo.cache";
    private static final char ESCAPE = '%';
    private static final Set<Character> LEGAL_CHARACTERS = Collections.unmodifiableSet(new HashSet<Character>(){{
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
        if (basePath == null) {
            basePath = System.getProperty("user.home") + File.separator + ".dubbo";
        }
        if (basePath.endsWith(File.separator)) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        File candidate = new File(basePath);
        // ensure cache store path exists
        if (!candidate.isDirectory() && !candidate.mkdirs()) {
            throw new RuntimeException("Cache store path can't be created: " + candidate);
        }

        cacheName = safeName(cacheName);
        if (!cacheName.endsWith(SUFFIX)) {
            cacheName = cacheName + SUFFIX;
        }

        String cacheFilePath = basePath + File.separator + cacheName;

        return cacheMap.computeIfAbsent(cacheFilePath, (k) -> getFile(k));
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
                sb.append(ESCAPE);
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
    private static FileCacheStore getFile(String name) {
        try {
            FileCacheStore.Builder builder = FileCacheStore.newBuilder();
            tryFileLock(builder, name);
            File file = new File(name);
            if (!file.exists()) {
                file.createNewFile();
            }

            builder.cacheFilePath(name)
                .cacheFile(file);
            return builder.build();
        } catch (Throwable t) {
            logger.info("Failed to create file store cache. Local file cache will be disabled. Cache file name: " + name, t);
            return FileCacheStore.Empty.getInstance(name);
        }
    }

    private static void tryFileLock(FileCacheStore.Builder builder, String fileName) throws PathNotExclusiveException {
        File lockFile = new File(fileName + ".lock");
        lockFile.deleteOnExit();

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
            throw new PathNotExclusiveException(fileName + " is not exclusive.");
        }

        builder.directoryLock(dirLock).lockFile(lockFile);
    }

    protected static void removeCache(String cacheFileName) {
        cacheMap.remove(cacheFileName);
    }

    /**
     * for unit test only
     */
    @Deprecated
    protected static Map<String, FileCacheStore> getCacheMap() {
        return cacheMap;
    }

    private static class PathNotExclusiveException extends Exception {
        public PathNotExclusiveException(String msg) {
            super(msg);
        }
    }

}
