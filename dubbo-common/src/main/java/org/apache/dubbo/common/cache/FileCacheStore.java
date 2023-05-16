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
import org.apache.dubbo.common.utils.CollectionUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_CACHE_MAX_ENTRY_COUNT_LIMIT_EXCEED;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_CACHE_MAX_FILE_SIZE_LIMIT_EXCEED;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_CACHE_PATH_INACCESSIBLE;

/**
 * Local file interaction class that can back different caches.
 * <p>
 * All items in local file are of human friendly format.
 */
public class FileCacheStore {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(FileCacheStore.class);

    private final String cacheFilePath;
    private final File cacheFile;
    private final File lockFile;
    private final FileLock directoryLock;

    private FileCacheStore(String cacheFilePath, File cacheFile, File lockFile, FileLock directoryLock) {
        this.cacheFilePath = cacheFilePath;
        this.cacheFile = cacheFile;
        this.lockFile = lockFile;
        this.directoryLock = directoryLock;
    }

    public synchronized Map<String, String> loadCache(int entrySize) throws IOException {
        Map<String, String> properties = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
            int count = 1;
            String line = reader.readLine();
            while (line != null && count <= entrySize) {
                // content has '=' need to be encoded before write
                if (!line.startsWith("#") && line.contains("=")) {
                    String[] pairs = line.split("=");
                    properties.put(pairs[0], pairs[1]);
                    count++;
                }
                line = reader.readLine();
            }

            if (count > entrySize) {
                logger.warn(COMMON_CACHE_MAX_FILE_SIZE_LIMIT_EXCEED, "mis-configuration of system properties",
                    "Check Java system property 'dubbo.mapping.cache.entrySize' and 'dubbo.meta.cache.entrySize'.",
                    "Cache file was truncated for exceeding the maximum entry size: " + entrySize);
            }
        } catch (IOException e) {
            logger.warn(COMMON_CACHE_PATH_INACCESSIBLE, "inaccessible of cache path", "",
                "Load cache failed ", e);

            throw e;
        }
        return properties;
    }


    private void unlock() {
        if (directoryLock != null && directoryLock.isValid()) {
            try {
                directoryLock.release();
                directoryLock.channel().close();
                deleteFile(lockFile);
            } catch (IOException e) {
                logger.error(COMMON_CACHE_PATH_INACCESSIBLE, "inaccessible of cache path", "",
                    "Failed to release cache path's lock file:" + lockFile, e);

                throw new RuntimeException("Failed to release cache path's lock file:" + lockFile, e);
            }
        }
    }

    public synchronized void refreshCache(Map<String, String> properties, String comment, long maxFileSize) {
        if (CollectionUtils.isEmptyMap(properties)) {
            return;
        }

        try (LimitedLengthBufferedWriter bw =
                 new LimitedLengthBufferedWriter(
                     new OutputStreamWriter(
                         new FileOutputStream(cacheFile, false), StandardCharsets.UTF_8), maxFileSize)) {

            bw.write("#" + comment);
            bw.newLine();
            bw.write("#" + new Date());
            bw.newLine();

            for (Map.Entry<String, String> e : properties.entrySet()) {
                String key = e.getKey();
                String val = e.getValue();
                bw.write(key + "=" + val);
                bw.newLine();
            }

            bw.flush();

            long remainSize = bw.getRemainSize();
            if (remainSize < 0) {
                logger.warn(COMMON_CACHE_MAX_ENTRY_COUNT_LIMIT_EXCEED, "mis-configuration of system properties",
                    "Check Java system property 'dubbo.mapping.cache.maxFileSize' and 'dubbo.meta.cache.maxFileSize'.",
                    "Cache file was truncated for exceeding the maximum file size " + maxFileSize + " byte. Exceeded by " + (-remainSize) + " byte.");
            }
        } catch (IOException e) {
            logger.warn(COMMON_CACHE_PATH_INACCESSIBLE, "inaccessible of cache path", "",
                "Update cache error.", e);
        }
    }

    private static void deleteFile(File f) {

        Path pathOfFile = f.toPath();

        try {
            Files.delete(pathOfFile);
        } catch (IOException ioException) {
            logger.debug("Failed to delete file " + f.getAbsolutePath(), ioException);
        }
    }

    public synchronized void destroy() {
        unlock();
        FileCacheStoreFactory.removeCache(cacheFilePath);
    }

    /**
     * for unit test only
     */
    @Deprecated
    protected String getCacheFilePath() {
        return cacheFilePath;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String cacheFilePath;
        private File cacheFile;
        private File lockFile;
        private FileLock directoryLock;

        private Builder() {
        }

        public Builder cacheFilePath(String cacheFilePath) {
            this.cacheFilePath = cacheFilePath;
            return this;
        }

        public Builder cacheFile(File cacheFile) {
            this.cacheFile = cacheFile;
            return this;
        }

        public Builder lockFile(File lockFile) {
            this.lockFile = lockFile;
            return this;
        }

        public Builder directoryLock(FileLock directoryLock) {
            this.directoryLock = directoryLock;
            return this;
        }

        public FileCacheStore build() {
            return new FileCacheStore(cacheFilePath, cacheFile, lockFile, directoryLock);
        }
    }

    /**
     * An empty (or fallback) implementation of FileCacheStore. Used when cache file creation failed.
     */
    protected static class Empty extends FileCacheStore {

        private Empty(String cacheFilePath) {
            super(cacheFilePath, null, null, null);
        }

        public static Empty getInstance(String cacheFilePath) {
            return new Empty(cacheFilePath);
        }

        @Override
        public synchronized Map<String, String> loadCache(int entrySize) throws IOException {
            return Collections.emptyMap();
        }

        @Override
        public synchronized void refreshCache(Map<String, String> properties, String comment, long maxFileSize) {
            // No-op.
        }
    }

    /**
     * A BufferedWriter which limits the length (in bytes). When limit exceed, this writer stops writing.
     */
    private static class LimitedLengthBufferedWriter extends BufferedWriter {

        private long remainSize;

        public LimitedLengthBufferedWriter(Writer out, long maxSize) {
            super(out);
            this.remainSize = maxSize == 0 ? Long.MAX_VALUE : maxSize;
        }

        @Override
        public void write(String str) throws IOException {
            remainSize -= str.getBytes(StandardCharsets.UTF_8).length;
            if (remainSize < 0) {
                return;
            }
            super.write(str);
        }

        public long getRemainSize() {
            return remainSize;
        }
    }
}
