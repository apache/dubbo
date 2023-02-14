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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.system.OperatingSystemBeanManager;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.MD5Utils;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_CACHE_PATH_INACCESSIBLE;
import static org.apache.dubbo.common.system.OperatingSystemBeanManager.OS.Windows;

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

    public static final String SUFFIX = ".dubbo.cache";
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

    public static FileCacheStore getInstance(String basePath, String filePrefix, String cacheName) {
        return getInstance(basePath, filePrefix, cacheName, true);
    }

    public static FileCacheStore getInstance(String basePath, String filePrefix, String cacheName, boolean enableFileCache) {
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
        OperatingSystemBeanManager.OS os = OperatingSystemBeanManager.getOS();
        if (os == Windows || "true".equals(System.getProperty(CommonConstants.File_ADDRESS_SHORTENED, "false"))) {
            MD5Utils md5Utils = new MD5Utils();
            /** try to shorten the address
             *  for example,  basePath: /Users/aming/.dubbo   cacheName: .metadata.dubbo-demo-api-provider-2.zookeeper.127.0.0.1%003a2181.dubbo.cache
             *  and the fileContent = /Users/aming/.dubbo/.metadata.dubbo-demo-api-provider-2.zookeeper.127.0.0.1%003a2181.dubbo.cache
             *      the basePath = /Users/aming/.dubbo/.metadata
             *      the cacheFilePath = /Users/aming/.dubbo/.metadata/b5c91baccb83c8786f7e33a84e1c417e
             */
            String fileContent = basePath + File.separator + cacheName;
            basePath = basePath + File.separator + filePrefix;
            String md5String32Bit = md5Utils.getMd5(cacheName);
            String cacheFilePath = basePath + "." + md5Utils.getMd5(cacheName);
            return ConcurrentHashMapUtils.computeIfAbsent(cacheMap, cacheFilePath, k -> getFile(cacheFilePath, fileContent, md5String32Bit, enableFileCache, true));
        } else {
            String cacheFilePath = basePath + File.separator + cacheName;
            return ConcurrentHashMapUtils.computeIfAbsent(cacheMap, cacheFilePath, k -> getFile(cacheFilePath, "", "", enableFileCache, true));
        }
    }

    /**
     * sanitize a name for valid file or directory name
     *
     * @param name origin file name
     * @return sanitized version of name
     */
    static String safeName(String name) {
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
    private static FileCacheStore getFile(String name, String fileContent, String md5String, boolean enableFileCache, boolean enableAddressShorten) {
        if (!enableFileCache) {
            return FileCacheStore.Empty.getInstance(name);
        }
        FileCacheStore.Builder builder = FileCacheStore.newBuilder();
        try {
            tryFileLock(builder, name, md5String, enableAddressShorten);
            getFile(builder, name, fileContent, md5String, enableFileCache, enableAddressShorten);
        } catch (Throwable t) {
            logger.warn(COMMON_CACHE_PATH_INACCESSIBLE, "inaccessible of cache path", "",
                "Failed to create file store cache. Local file cache will be disabled. Cache file name: " + name, t);
            return FileCacheStore.Empty.getInstance(name);
        }
        return builder.build();
    }

    private static void getFile(FileCacheStore.Builder builder, String name, String fileContent, String md5String, boolean enableFileCache, boolean enableAddressShorten) {
        try {
            File file = new File(name);
            if (!file.exists()) {
                Path pathObjectOfFile = file.toPath();
                try {
                    Files.createFile(pathObjectOfFile);
                } catch (FileSystemException e) {
                    if (enableAddressShorten && !StringUtils.isEmpty(md5String) && md5String.length() == 32) {
                        int newNameIndex = name.indexOf(md5String);
                        if (newNameIndex == -1) {
                            logger.warn(COMMON_CACHE_PATH_INACCESSIBLE, "inaccessible of cache path", "",
                                "Failed to create file store cache. Local file cache will be disabled. Cache file name: " + name, e);
                            return;
                        }
                        String md5String16Bit = md5String.substring(8, 24);
                        String newName = name.substring(0, newNameIndex - 1) + "." + md5String16Bit;
                        getFile(builder, newName, fileContent, md5String16Bit, enableFileCache, enableAddressShorten);
                        return;
                    } else {
                        logger.warn(COMMON_CACHE_PATH_INACCESSIBLE, "inaccessible of cache path", "",
                            "Failed to create file store cache. Local file cache will be disabled. Cache file name: " + name, e);
                        return ;
                    }
                }
            }
            if (!StringUtils.isEmpty(fileContent) && enableFileCache) {
                try (FileOutputStream outputFile = new FileOutputStream(file)) {
                    outputFile.write(fileContent.getBytes(StandardCharsets.UTF_8), 0, fileContent.length());
                }
            }

            builder.cacheFilePath(name)
                .cacheFile(file);
        } catch (Throwable t) {
            logger.warn(COMMON_CACHE_PATH_INACCESSIBLE, "inaccessible of cache path", "",
                "Failed to create file store cache. Local file cache will be disabled. Cache file name: " + name, t);
        }
    }

    private static void tryFileLock(FileCacheStore.Builder builder, String fileName, String md5String, boolean enableAddressShorten) throws PathNotExclusiveException {
        File lockFile = new File(fileName + ".lock");

        FileLock dirLock = null;
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
            if (enableAddressShorten && !StringUtils.isEmpty(md5String) && md5String.length() == 32) {
                int newNameIndex = fileName.indexOf(md5String);
                if (newNameIndex == -1) {
                    throw new RuntimeException(ioe);
                }
                String md5String16Bit = md5String.substring(8, 24);
                String newName = fileName.substring(0, newNameIndex - 1) + "." + md5String16Bit;
                lockFile.deleteOnExit();
                tryFileLock(builder, newName, md5String16Bit, enableAddressShorten);
                return;
            } else {
                throw new RuntimeException(ioe);
            }
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
