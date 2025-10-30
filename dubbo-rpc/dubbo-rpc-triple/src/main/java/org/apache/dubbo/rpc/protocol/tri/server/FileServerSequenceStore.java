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
package org.apache.dubbo.rpc.protocol.tri.server;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * File-based implementation of ServerSequenceStore
 * 文件实现的服务端序列号存储，支持跨进程重启的持久化
 */
public class FileServerSequenceStore implements ServerSequenceStore {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(FileServerSequenceStore.class);

    private volatile String currentSessionId;
    private volatile String currentServiceKey;
    private volatile Path storeFile;
    private volatile boolean initialized = false;

    // In-memory cache for performance
    private final ConcurrentSkipListSet<Long> processedSequences = new ConcurrentSkipListSet<>();
    private final AtomicLong lastAcknowledgedSequence = new AtomicLong(-1);
    private final AtomicLong totalSequencesRecorded = new AtomicLong(0);
    private final AtomicLong cleanupCount = new AtomicLong(0);

    // File operations lock
    private final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();

    // Store directory
    private static final String DEFAULT_STORE_DIR = System.getProperty("user.home") + "/.dubbo/tri/sequences";
    private final String storeDirectory;

    public FileServerSequenceStore() {
        this(DEFAULT_STORE_DIR);
    }

    public FileServerSequenceStore(String storeDirectory) {
        this.storeDirectory = storeDirectory;
    }

    @Override
    public void init(String sessionId, String serviceKey) throws ServerSequenceException {
        try {
            this.currentSessionId = sessionId;
            this.currentServiceKey = serviceKey;

            // Create store directory if it doesn't exist
            Path dirPath = Paths.get(storeDirectory);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // Create store file path
            this.storeFile = dirPath.resolve(sessionId + "_" + serviceKey.replace(".", "_") + ".seq");

            // Load existing data if file exists
            if (Files.exists(storeFile)) {
                loadFromFile();
            }

            this.initialized = true;
            LOGGER.info("Initialized FileServerSequenceStore for session: {} at file: {}", sessionId, storeFile);
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to initialize file store for session: " + sessionId, e);
        }
    }

    @Override
    public void recordSequence(long sequence) throws ServerSequenceException {
        if (!initialized) {
            throw new ServerSequenceException("Store not initialized");
        }

        try {
            // Check if already processed (in-memory cache)
            if (processedSequences.contains(sequence)) {
                return;
            }

            // Add to in-memory cache
            if (processedSequences.add(sequence)) {
                totalSequencesRecorded.incrementAndGet();
                lastAcknowledgedSequence.updateAndGet(current -> Math.max(current, sequence));

                // Persist to file
                persistToFile();
            }
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to record sequence: " + sequence, e);
        }
    }

    @Override
    public long getLastAcknowledgedSequence() throws ServerSequenceException {
        if (!initialized) {
            return -1;
        }
        return lastAcknowledgedSequence.get();
    }

    @Override
    public boolean isSequenceProcessed(long sequence) throws ServerSequenceException {
        if (!initialized) {
            return false;
        }

        // First check in-memory cache
        if (processedSequences.contains(sequence)) {
            return true;
        }

        // If not in cache, check file (this handles case where cache was cleared but file exists)
        try {
            fileLock.readLock().lock();
            if (Files.exists(storeFile)) {
                try (BufferedReader reader = Files.newBufferedReader(storeFile)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        try {
                            long seq = Long.parseLong(line.trim());
                            if (seq == sequence) {
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                            LOGGER.warn(
                                    LoggerCodeConstants.INTERNAL_ERROR,
                                    "",
                                    "",
                                    "Invalid sequence number in file: " + line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to check sequence from file", e);
        } finally {
            fileLock.readLock().unlock();
        }

        return false;
    }

    @Override
    public void cleanupSequencesUpTo(long sequence) throws ServerSequenceException {
        if (!initialized) {
            return;
        }

        try {
            // Remove from in-memory cache
            if (processedSequences.headSet(sequence).size() > 0) {
                processedSequences.headSet(sequence).clear();
                cleanupCount.incrementAndGet();
            }

            // Rebuild file with remaining sequences
            persistToFile();
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to cleanup sequences up to: " + sequence, e);
        }
    }

    @Override
    public void cleanupAcknowledged(long sequence) throws ServerSequenceException {
        if (!initialized) {
            return;
        }

        try {
            // Remove acknowledged sequences from in-memory cache
            if (processedSequences.headSet(sequence).size() > 0) {
                int cleanedCount = processedSequences.headSet(sequence).size();
                processedSequences.headSet(sequence).clear();
                cleanupCount.incrementAndGet();

                // Update last acknowledged sequence if needed
                lastAcknowledgedSequence.updateAndGet(current -> Math.max(current, sequence));

                LOGGER.info(
                        "Cleaned up {} acknowledged sequences up to: {} for session: {}",
                        cleanedCount,
                        sequence,
                        currentSessionId);
            }

            // Rebuild file with remaining sequences (only unacknowledged ones)
            persistToFile();
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to cleanup acknowledged sequences up to: " + sequence, e);
        }
    }

    @Override
    public StoreStats getStats() throws ServerSequenceException {
        if (!initialized) {
            return new StoreStats(0, 0, -1, 0);
        }

        return new StoreStats(
                totalSequencesRecorded.get(),
                processedSequences.size(),
                lastAcknowledgedSequence.get(),
                cleanupCount.get());
    }

    @Override
    public void close() throws ServerSequenceException {
        try {
            if (initialized && storeFile != null) {
                // Final persist before closing
                persistToFile();

                // Clear in-memory cache
                int clearedCount = processedSequences.size();
                processedSequences.clear();

                LOGGER.info(
                        "Closed FileServerSequenceStore for session: {}, cleared {} sequences",
                        currentSessionId,
                        clearedCount);
            }
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to close file store for session: " + currentSessionId, e);
        } finally {
            initialized = false;
        }
    }

    @Override
    public boolean isHealthy() {
        if (!initialized || storeFile == null) {
            return false;
        }

        // Check if we can write to the store directory
        try {
            Path testFile = Paths.get(storeDirectory, "health_test_" + System.currentTimeMillis() + ".tmp");
            Files.write(testFile, "test".getBytes());
            Files.deleteIfExists(testFile);
            return true;
        } catch (Exception e) {
            LOGGER.warn(
                    LoggerCodeConstants.INTERNAL_ERROR,
                    "",
                    "",
                    "File store health check failed for directory: " + storeDirectory,
                    e);
            return false;
        }
    }

    /**
     * Persist current state to file
     */
    private void persistToFile() throws ServerSequenceException {
        fileLock.writeLock().lock();
        try {
            // Write to temporary file first, then rename for atomicity
            Path tempFile = Paths.get(storeFile.toString() + ".tmp");

            try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
                for (Long sequence : processedSequences) {
                    writer.write(sequence.toString());
                    writer.newLine();
                }
            }

            // Atomic replace
            Files.move(tempFile, storeFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            throw new ServerSequenceException("Failed to persist sequences to file: " + storeFile, e);
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    /**
     * Load existing data from file
     */
    private void loadFromFile() throws ServerSequenceException {
        fileLock.readLock().lock();
        try {
            try (BufferedReader reader = Files.newBufferedReader(storeFile)) {
                String line;
                long maxSeq = -1;
                int count = 0;

                while ((line = reader.readLine()) != null) {
                    try {
                        long sequence = Long.parseLong(line.trim());
                        processedSequences.add(sequence);
                        maxSeq = Math.max(maxSeq, sequence);
                        count++;
                    } catch (NumberFormatException e) {
                        LOGGER.warn(
                                LoggerCodeConstants.INTERNAL_ERROR,
                                "",
                                "",
                                "Invalid sequence number in file: " + line + ", skipping");
                    }
                }

                if (maxSeq >= 0) {
                    lastAcknowledgedSequence.set(maxSeq);
                }
                totalSequencesRecorded.set(count);

                LOGGER.info("Loaded {} sequences from file: {}", count, storeFile);
            }
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to load sequences from file: " + storeFile, e);
        } finally {
            fileLock.readLock().unlock();
        }
    }

    /**
     * Get the store file path (for testing and monitoring)
     */
    public Path getStoreFilePath() {
        return storeFile;
    }

    /**
     * Get the store directory (for testing and monitoring)
     */
    public String getStoreDirectory() {
        return storeDirectory;
    }

    /**
     * Force immediate persistence (for testing)
     */
    public void forcePersist() throws ServerSequenceException {
        persistToFile();
    }

    /**
     * Clear all data (for testing)
     */
    public void clear() throws ServerSequenceException {
        fileLock.writeLock().lock();
        try {
            processedSequences.clear();
            lastAcknowledgedSequence.set(-1);
            totalSequencesRecorded.set(0);
            cleanupCount.set(0);

            if (storeFile != null) {
                Files.deleteIfExists(storeFile);
            }

            LOGGER.info("Cleared all data from FileServerSequenceStore for session: {}", currentSessionId);
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to clear file store", e);
        } finally {
            fileLock.writeLock().unlock();
        }
    }
}
