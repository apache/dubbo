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
package org.apache.dubbo.rpc.protocol.tri.store;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File-based implementation of PendingMessageStore.
 * Provides persistence across process restarts using append-only log files.
 */
public class FilePendingMessageStore implements PendingMessageStore {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(FilePendingMessageStore.class);

    private static final String FILE_SUFFIX = ".pmsg";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final int MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long DEFAULT_RETENTION_TIME = 24 * 60 * 60 * 1000; // 24 hours

    // ACK cleanup constants
    private static final int CLEANUP_BATCH_SIZE = 100; // Clean up after every 100 ACKs
    private static final int CLEANUP_INTERVAL = 1000; // Clean up every 1000 sequences
    private static final double COMPRESSION_THRESHOLD = 0.3; // Compress when 30% of messages are acknowledged

    private final String basePath;
    private final long maxFileSize;
    private final long retentionTime;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    private SessionMetadata currentMetadata;
    private Path currentFile;
    private DataOutputStream outputStream;

    // ACK tracking fields
    private final AtomicLong lastAcknowledgedSequence = new AtomicLong(-1);
    private final AtomicLong totalMessagesRecorded = new AtomicLong(0);
    private final AtomicLong cleanupCount = new AtomicLong(0);

    public FilePendingMessageStore() {
        this("/tmp/dubbo/reliable", MAX_FILE_SIZE, DEFAULT_RETENTION_TIME);
    }

    public FilePendingMessageStore(String basePath, long maxFileSize, long retentionTime) {
        this.basePath = basePath;
        this.maxFileSize = maxFileSize;
        this.retentionTime = retentionTime;
    }

    @Override
    public void init(SessionMetadata metadata) throws StoreException {
        lock.writeLock().lock();
        try {
            this.currentMetadata = metadata;

            // Create base directory if it doesn't exist
            Path baseDir = Paths.get(basePath);
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }

            // Create session-specific directory
            Path sessionDir = baseDir.resolve(metadata.getSessionId());
            if (!Files.exists(sessionDir)) {
                Files.createDirectories(sessionDir);
            }

            // Initialize current file
            currentFile = sessionDir.resolve(System.currentTimeMillis() + FILE_SUFFIX);
            outputStream =
                    new DataOutputStream(new BufferedOutputStream(new FileOutputStream(currentFile.toFile(), true)));

            // Clean up old files
            cleanupOldFiles(sessionDir);

            LOGGER.info(
                    "Initialized FilePendingMessageStore for session: {} at path: {}",
                    metadata.getSessionId(),
                    currentFile);
        } catch (Exception e) {
            healthy.set(false);
            throw new StoreException("Failed to initialize store for session: " + metadata.getSessionId(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void put(PendingMessage message) throws StoreException {
        if (!healthy.get()) {
            throw new StoreException("Store is not healthy");
        }

        lock.writeLock().lock();
        try {
            // Check if we need to rotate file
            if (Files.size(currentFile) > maxFileSize) {
                rotateFile();
            }

            // Write message to file
            writeMessage(message);
            totalMessagesRecorded.incrementAndGet();

            LOGGER.debug(
                    "Stored message with sequence: {} for session: {}", message.getSequence(), message.getSessionId());
        } catch (Exception e) {
            healthy.set(false);
            throw new StoreException("Failed to store message: " + message.getSequence(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void ack(long sequence) throws StoreException {
        if (currentMetadata == null) {
            LOGGER.warn(
                    LoggerCodeConstants.INTERNAL_ERROR,
                    "",
                    "",
                    "Store not initialized, cannot ack sequence: " + sequence);
            return;
        }

        try {
            // Update the last acknowledged sequence
            long previousAckedSeq = lastAcknowledgedSequence.getAndSet(sequence);

            // Only trigger cleanup if this is a new acknowledgment
            if (sequence > previousAckedSeq) {
                // For efficiency, we batch cleanup operations
                // Only trigger file cleanup when we have accumulated enough acknowledged messages
                long acknowledgedCount = sequence - previousAckedSeq;
                if (acknowledgedCount >= CLEANUP_BATCH_SIZE || sequence % CLEANUP_INTERVAL == 0) {
                    cleanupAcknowledgedMessages(sequence);
                }
            }

            LOGGER.debug(
                    "Acknowledged message with sequence: {} for session: {}", sequence, currentMetadata.getSessionId());
        } catch (Exception e) {
            throw new StoreException("Failed to acknowledge sequence: " + sequence, e);
        }
    }

    @Override
    public void remove(long sequence) throws StoreException {
        // For append-only log, we don't remove immediately
        // Cleanup happens during load and file rotation
        LOGGER.debug("Removed message with sequence: {} for session: {}", sequence, currentMetadata.getSessionId());
    }

    @Override
    public List<PendingMessage> load(String sessionId) throws StoreException {
        lock.readLock().lock();
        try {
            List<PendingMessage> messages = new ArrayList<>();
            Path sessionDir = Paths.get(basePath).resolve(sessionId);

            if (!Files.exists(sessionDir)) {
                LOGGER.debug("No session directory found for session: {}", sessionId);
                return messages;
            }

            // Read all files for the session
            List<PendingMessage> fileMessages = Files.list(sessionDir)
                    .filter(path -> path.toString().endsWith(FILE_SUFFIX))
                    .flatMap(path -> {
                        try {
                            return readMessagesFromFile(path).stream();
                        } catch (Exception e) {
                            LOGGER.warn(
                                    LoggerCodeConstants.INTERNAL_ERROR,
                                    "",
                                    "",
                                    "Failed to read messages from file: " + path,
                                    e);
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toList());
            messages.addAll(fileMessages);

            // Deduplicate by keeping the latest version of each sequence
            messages = deduplicateMessages(messages);

            LOGGER.info("Loaded {} unique messages for session: {}", messages.size(), sessionId);
            return messages;
        } catch (Exception e) {
            throw new StoreException("Failed to load messages for session: " + sessionId, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void close(SessionMetadata metadata) throws StoreException {
        lock.writeLock().lock();
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }

            LOGGER.info("Closed FilePendingMessageStore for session: {}", metadata.getSessionId());
        } catch (Exception e) {
            throw new StoreException("Failed to close store for session: " + metadata.getSessionId(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean isHealthy() {
        return healthy.get();
    }

    private void writeMessage(PendingMessage message) throws IOException {
        writeMessageToStream(outputStream, message);
    }

    private void writeMessageToStream(DataOutputStream out, PendingMessage message) throws IOException {
        // Write message format (same as existing format):
        // [sequence][messageLength][message][compressFlag][sentTime][retryCount][firstSendTime][nextRetryDelay][sessionIdLength][sessionId]

        byte[] messageData = message.getMessage();
        byte[] sessionIdData = message.getSessionId().getBytes();

        out.writeLong(message.getSequence());
        out.writeInt(messageData.length);
        out.write(messageData);
        out.writeInt(message.getCompressFlag());
        out.writeLong(message.getSentTime());
        out.writeInt(message.getRetryCount());
        out.writeLong(message.getFirstSendTime());
        out.writeLong(message.getNextRetryDelay());
        out.writeInt(sessionIdData.length);
        out.write(sessionIdData);
    }

    private List<PendingMessage> readMessagesFromFile(Path path) throws IOException {
        List<PendingMessage> messages = new ArrayList<>();

        try (DataInputStream inputStream = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path.toFile().toPath())))) {

            while (inputStream.available() > 0) {
                try {
                    long sequence = inputStream.readLong();
                    int messageLength = inputStream.readInt();
                    byte[] messageData = new byte[messageLength];
                    inputStream.readFully(messageData);
                    int compressFlag = inputStream.readInt();
                    long sentTime = inputStream.readLong();
                    int retryCount = inputStream.readInt();
                    long firstSendTime = inputStream.readLong();
                    long nextRetryDelay = inputStream.readLong();
                    int sessionIdLength = inputStream.readInt();
                    byte[] sessionIdData = new byte[sessionIdLength];
                    inputStream.readFully(sessionIdData);
                    String sessionId = new String(sessionIdData);

                    PendingMessage message = new PendingMessage(
                            sequence,
                            messageData,
                            compressFlag,
                            sentTime,
                            retryCount,
                            firstSendTime,
                            nextRetryDelay,
                            sessionId);

                    messages.add(message);
                } catch (EOFException e) {
                    // End of file reached
                    break;
                }
            }
        }

        return messages;
    }

    private List<PendingMessage> deduplicateMessages(List<PendingMessage> messages) {
        // Simple deduplication: keep the latest version of each sequence
        Map<Long, PendingMessage> latestMessages = new HashMap<>();

        for (PendingMessage message : messages) {
            PendingMessage existing = latestMessages.get(message.getSequence());
            if (existing == null || message.getSentTime() > existing.getSentTime()) {
                latestMessages.put(message.getSequence(), message);
            }
        }

        return new ArrayList<>(latestMessages.values());
    }

    private void rotateFile() throws IOException {
        lock.writeLock().lock();
        try {
            if (outputStream != null) {
                outputStream.close();
            }

            // Create new file
            Path sessionDir = Paths.get(basePath).resolve(currentMetadata.getSessionId());
            currentFile = sessionDir.resolve(System.currentTimeMillis() + FILE_SUFFIX);
            outputStream =
                    new DataOutputStream(new BufferedOutputStream(new FileOutputStream(currentFile.toFile(), true)));

            LOGGER.info("Rotated to new file: {} for session: {}", currentFile, currentMetadata.getSessionId());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void cleanupOldFiles(Path sessionDir) throws IOException {
        long cutoffTime = System.currentTimeMillis() - retentionTime;

        Files.list(sessionDir)
                .filter(path -> path.toString().endsWith(FILE_SUFFIX))
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        LOGGER.warn(
                                LoggerCodeConstants.INTERNAL_ERROR,
                                "",
                                "",
                                "Failed to get last modified time for file: " + path,
                                e);
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                        LOGGER.debug("Deleted old file: {}", path);
                    } catch (IOException e) {
                        LOGGER.warn(
                                LoggerCodeConstants.INTERNAL_ERROR, "", "", "Failed to delete old file: " + path, e);
                    }
                });
    }

    /**
     * Clean up acknowledged messages from the store
     * 清理已确认的消息
     */
    private void cleanupAcknowledgedMessages(long ackedSequence) throws StoreException {
        if (currentMetadata == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            Path sessionDir = Paths.get(basePath).resolve(currentMetadata.getSessionId());
            if (!Files.exists(sessionDir)) {
                return;
            }

            // Calculate the ratio of acknowledged messages
            long totalMsgs = totalMessagesRecorded.get();
            if (totalMsgs == 0) {
                return;
            }

            double ackRatio = (double) ackedSequence / totalMsgs;

            // Only perform cleanup if we have enough acknowledged messages
            if (ackRatio >= COMPRESSION_THRESHOLD) {
                LOGGER.info(
                        "Starting cleanup for session: {}, acked: {}, total: {}, ratio: {}",
                        currentMetadata.getSessionId(),
                        ackedSequence,
                        totalMsgs,
                        ackRatio);

                // Create a new compressed file containing only unacknowledged messages
                Path compressedFile = sessionDir.resolve("compressed_" + System.currentTimeMillis() + FILE_SUFFIX);

                try (DataOutputStream compressedOut =
                        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(compressedFile.toFile())))) {

                    // Read all files and filter out acknowledged messages
                    List<Path> messageFiles = Files.list(sessionDir)
                            .filter(path -> path.toString().endsWith(FILE_SUFFIX))
                            .sorted()
                            .collect(Collectors.toList());

                    long unacknowledgedCount = 0;
                    long acknowledgedCount = 0;

                    for (Path file : messageFiles) {
                        try {
                            // Use existing readMessagesFromFile method to read with correct format
                            List<PendingMessage> messages = readMessagesFromFile(file);
                            for (PendingMessage message : messages) {
                                if (message.getSequence() > ackedSequence) {
                                    // Write unacknowledged message to compressed file using existing format
                                    writeMessageToStream(compressedOut, message);
                                    unacknowledgedCount++;
                                } else {
                                    acknowledgedCount++;
                                }
                            }
                        } catch (IOException e) {
                            LOGGER.warn(
                                    LoggerCodeConstants.INTERNAL_ERROR,
                                    "",
                                    "",
                                    "Failed to read messages from file during cleanup: " + file,
                                    e);
                        }
                    }

                    // Replace old files with compressed file
                    if (unacknowledgedCount > 0 || acknowledgedCount > 0) {
                        // Delete old files
                        for (Path file : messageFiles) {
                            try {
                                Files.deleteIfExists(file);
                            } catch (IOException e) {
                                LOGGER.warn(
                                        LoggerCodeConstants.INTERNAL_ERROR,
                                        "",
                                        "",
                                        "Failed to delete old file during cleanup: " + file,
                                        e);
                            }
                        }

                        // Rename compressed file to be the new current file
                        Path newCurrentFile = sessionDir.resolve(System.currentTimeMillis() + FILE_SUFFIX);
                        Files.move(compressedFile, newCurrentFile);

                        // Update current file reference
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        currentFile = newCurrentFile;
                        outputStream = new DataOutputStream(
                                new BufferedOutputStream(new FileOutputStream(currentFile.toFile(), true)));

                        cleanupCount.incrementAndGet();

                        LOGGER.info(
                                "Cleanup completed for session: {}, removed: {}, kept: {}",
                                currentMetadata.getSessionId(),
                                acknowledgedCount,
                                unacknowledgedCount);
                    } else {
                        // No messages found, delete the empty compressed file
                        Files.deleteIfExists(compressedFile);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(
                    LoggerCodeConstants.INTERNAL_ERROR,
                    "",
                    "",
                    "Failed to cleanup acknowledged messages up to: " + ackedSequence + " for session: "
                            + currentMetadata.getSessionId(),
                    e);
            throw new StoreException("Failed to cleanup acknowledged messages", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public SessionMetadata loadMetadata(String sessionId) throws StoreException {
        lock.readLock().lock();
        try {
            Path baseDir = Paths.get(basePath);
            Path sessionDir = baseDir.resolve(sessionId);
            Path metadataFile = sessionDir.resolve("metadata.json");

            if (!Files.exists(metadataFile)) {
                LOGGER.debug("No metadata file found for session: {}", sessionId);
                return null;
            }

            // Read and parse metadata file
            String jsonContent = new String(Files.readAllBytes(metadataFile), StandardCharsets.UTF_8);
            return parseMetadataJson(jsonContent);

        } catch (Exception e) {
            LOGGER.error(
                    LoggerCodeConstants.INTERNAL_ERROR, "", "", "Failed to load metadata for session: " + sessionId, e);
            // Return null instead of throwing to allow graceful fallback
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void updateMetadata(SessionMetadata metadata) throws StoreException {
        lock.writeLock().lock();
        try {
            Path baseDir = Paths.get(basePath);
            Path sessionDir = baseDir.resolve(metadata.getSessionId());

            if (!Files.exists(sessionDir)) {
                Files.createDirectories(sessionDir);
            }

            Path metadataFile = sessionDir.resolve("metadata.json");
            Path tempFile = sessionDir.resolve("metadata.json.tmp");

            // Write to temp file first for atomic update
            String jsonContent = toMetadataJson(metadata);
            Files.write(
                    tempFile,
                    jsonContent.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.SYNC);

            // Atomic rename
            Files.move(tempFile, metadataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            LOGGER.debug("Updated metadata for session: {}", metadata.getSessionId());

        } catch (Exception e) {
            LOGGER.error(
                    LoggerCodeConstants.INTERNAL_ERROR,
                    "",
                    "",
                    "Failed to update metadata for session: " + metadata.getSessionId(),
                    e);
            throw new StoreException("Failed to update metadata", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Convert SessionMetadata to JSON format
     */
    private String toMetadataJson(SessionMetadata metadata) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"sessionId\": \"").append(metadata.getSessionId()).append("\",\n");
        json.append("  \"serviceKey\": \"").append(metadata.getServiceKey()).append("\",\n");
        json.append("  \"createTime\": ").append(metadata.getCreateTime()).append(",\n");
        json.append("  \"lastAckedSeq\": ").append(metadata.getLastAckedSeq()).append(",\n");
        json.append("  \"currentSeq\": ").append(metadata.getCurrentSeq()).append(",\n");
        json.append("  \"lastUpdateTime\": ")
                .append(metadata.getLastUpdateTime())
                .append("\n");
        json.append("}");
        return json.toString();
    }

    /**
     * Parse JSON to SessionMetadata
     */
    private SessionMetadata parseMetadataJson(String jsonContent) {
        // Simple JSON parsing without external dependencies
        try {
            String sessionId = extractJsonString(jsonContent, "sessionId");
            String serviceKey = extractJsonString(jsonContent, "serviceKey");
            long createTime = extractJsonLong(jsonContent, "createTime");
            long lastAckedSeq = extractJsonLong(jsonContent, "lastAckedSeq");
            long currentSeq = extractJsonLong(jsonContent, "currentSeq");
            long lastUpdateTime = extractJsonLong(jsonContent, "lastUpdateTime");

            // Reconstruct URL and config from currentMetadata if available
            if (currentMetadata != null) {
                return new SessionMetadata(
                        sessionId,
                        serviceKey,
                        createTime,
                        currentMetadata.getUrl(),
                        currentMetadata.getConfig(),
                        lastAckedSeq,
                        currentSeq,
                        lastUpdateTime);
            } else {
                // If no currentMetadata, return partial metadata (will be enriched later)
                LOGGER.warn(
                        LoggerCodeConstants.INTERNAL_ERROR,
                        "",
                        "",
                        "No current metadata available, returning partial metadata for session: " + sessionId);
                return null;
            }
        } catch (Exception e) {
            LOGGER.error(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Failed to parse metadata JSON", e);
            return null;
        }
    }

    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\": \"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) {
            return "";
        }
        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        return json.substring(startIndex, endIndex);
    }

    private long extractJsonLong(String json, String key) {
        String searchKey = "\"" + key + "\": ";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) {
            return 0L;
        }
        startIndex += searchKey.length();
        int endIndex = json.indexOf(",", startIndex);
        if (endIndex == -1) {
            endIndex = json.indexOf("\n", startIndex);
        }
        String value = json.substring(startIndex, endIndex).trim();
        return Long.parseLong(value);
    }
}
