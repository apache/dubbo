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
import org.apache.dubbo.common.utils.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Local file interaction class that can back different caches.
 *
 * All items in local file are of human friendly format.
 */
public class FileCacheStore {
    private static final Logger logger = LoggerFactory.getLogger(FileCacheStore.class);

    private static final int DEL = 0x7F;
    private static final char ESCAPE = '%';
    private static final Set<Character> ILLEGALS = new HashSet<Character>();
    private static final String SUFFIX = ".dubbo.cache";

    private String fileName;
    private File basePath;
    private File cacheFile;
    private FileLock directoryLock;
    private File lockFile;

    public FileCacheStore(String basePath, String fileName) {
        if (basePath == null) {
            basePath = System.getProperty("user.home") + "/.dubbo/";
        }
        this.basePath = new File(basePath);
        this.fileName = fileName;
    }

    public Properties loadCache() throws IOException {
        this.cacheFile = getFile(fileName, SUFFIX);
        Properties properties = new Properties();
        if (!cacheFile.exists()) {
            cacheFile.createNewFile();
        }
        try (FileInputStream is = new FileInputStream(cacheFile)) {
            properties.load(is);
        } catch (IOException e) {
            logger.warn("Load cache failed ", e);
            throw e;
        }
        return properties;
    }

    public File getFile(String cacheName, String suffix) {
        cacheName = safeName(cacheName);
        if (!cacheName.endsWith(suffix)) {
            cacheName = cacheName + suffix;
        }
        return getFile(cacheName);
    }

    /**
     * Get a file object for the given name
     *
     * @param name the file name
     * @return a file object
     */
    public File getFile(String name) {
        synchronized (this) {
            File candidate = basePath;
            // ensure cache store path exists
            if (!candidate.isDirectory() && !candidate.mkdirs()) {
                throw new RuntimeException("Cache store path can't be created: " + candidate);
            }

            try {
                tryFileLock(name);
            } catch (PathNotExclusiveException e) {
                logger.warn("Path '" + basePath
                    + "' is already used by an existing Dubbo process.\n"
                    + "Please specify another one explicitly.");
            }
        }

        File file = new File(basePath, name);
        for (File parent = file.getParentFile(); parent != null; parent = parent.getParentFile()) {
            if (basePath.equals(parent)) {
                return file;
            }
        }

        throw new IllegalArgumentException("Attempted to access file outside the dubbo cache path");
    }

    /**
     * sanitize a name for valid file or directory name
     *
     * @param name
     * @return sanitized version of name
     */
    private static String safeName(String name) {
        int len = name.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = name.charAt(i);
            if (c <= ' ' || c >= DEL || (c >= 'A' && c <= 'Z') || ILLEGALS.contains(c) || c == ESCAPE) {
                sb.append(ESCAPE);
                sb.append(String.format("%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void tryFileLock(String fileName) throws PathNotExclusiveException {
        lockFile = new File(basePath.getAbsoluteFile(), fileName + ".lock");
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
            throw new PathNotExclusiveException(basePath.getAbsolutePath() + " is not exclusive.");
        }

        this.directoryLock = dirLock;
    }

    private void unlock() {
        if (directoryLock != null && directoryLock.isValid()) {
            try {
                directoryLock.release();
                directoryLock.channel().close();
                deleteFile(lockFile);
            } catch (IOException e) {
                throw new RuntimeException("Failed to release cache path's lock file:" + lockFile, e);
            }
        }
    }

    public void refreshCache(Properties properties) {
        if (CollectionUtils.isEmptyMap(properties)) {
            return;
        }

        try (FileOutputStream os = new FileOutputStream(cacheFile, false)) {
            properties.store(os, "");
        } catch (IOException e) {
            logger.warn("Update cache error.");
        }
    }

    private static void deleteFile(File f) {
        if (!f.delete()) {
            logger.debug("Failed to delete file " + f.getAbsolutePath());
        }
    }

    private static class PathNotExclusiveException extends Exception {
        public PathNotExclusiveException() {
            super();
        }

        public PathNotExclusiveException(String msg) {
            super(msg);
        }
    }

    public void destroy() {
        unlock();
    }
}
