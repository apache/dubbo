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
package org.apache.dubbo.xds.resource.route;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.protobuf.Duration;
import io.grpc.Status;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RetryPolicy
 */
class RetryPolicyTest {

    @Test
    void testRetryPolicyCreation() {
        // Arrange
        int maxAttempts = 3;
        List<Status.Code> retryableStatusCodes = Arrays.asList(Status.Code.UNAVAILABLE, Status.Code.CANCELLED);
        Duration initialBackoff = Duration.newBuilder().setSeconds(0).setNanos(25_000_000).build(); // 25ms
        Duration maxBackoff = Duration.newBuilder().setSeconds(0).setNanos(250_000_000).build(); // 250ms
        Duration perAttemptTimeout = Duration.newBuilder().setSeconds(5).build(); // 5s

        // Act
        RetryPolicy retryPolicy = new RetryPolicy(maxAttempts, retryableStatusCodes, initialBackoff, maxBackoff, perAttemptTimeout);

        // Assert
        assertEquals(maxAttempts, retryPolicy.getMaxAttempts());
        assertEquals(retryableStatusCodes, retryPolicy.getRetryableStatusCodes());
        assertEquals(initialBackoff, retryPolicy.getInitialBackoff());
        assertEquals(maxBackoff, retryPolicy.getMaxBackoff());
        assertEquals(perAttemptTimeout, retryPolicy.getPerAttemptRecvTimeout());
    }

    @Test
    void testRetryPolicyWithNullPerAttemptTimeout() {
        // Arrange
        int maxAttempts = 5;
        List<Status.Code> retryableStatusCodes = Collections.singletonList(Status.Code.DEADLINE_EXCEEDED);
        Duration initialBackoff = Duration.newBuilder().setSeconds(0).setNanos(10_000_000).build(); // 10ms
        Duration maxBackoff = Duration.newBuilder().setSeconds(1).build(); // 1s

        // Act
        RetryPolicy retryPolicy = new RetryPolicy(maxAttempts, retryableStatusCodes, initialBackoff, maxBackoff, null);

        // Assert
        assertEquals(maxAttempts, retryPolicy.getMaxAttempts());
        assertEquals(retryableStatusCodes, retryPolicy.getRetryableStatusCodes());
        assertEquals(initialBackoff, retryPolicy.getInitialBackoff());
        assertEquals(maxBackoff, retryPolicy.getMaxBackoff());
        assertNull(retryPolicy.getPerAttemptRecvTimeout());
    }

    @Test
    void testRetryPolicyWithEmptyStatusCodes() {
        // Arrange
        int maxAttempts = 2;
        List<Status.Code> emptyStatusCodes = Collections.emptyList();
        Duration initialBackoff = Duration.newBuilder().setSeconds(0).setNanos(50_000_000).build(); // 50ms
        Duration maxBackoff = Duration.newBuilder().setSeconds(0).setNanos(500_000_000).build(); // 500ms

        // Act
        RetryPolicy retryPolicy = new RetryPolicy(maxAttempts, emptyStatusCodes, initialBackoff, maxBackoff, null);

        // Assert
        assertEquals(maxAttempts, retryPolicy.getMaxAttempts());
        assertTrue(retryPolicy.getRetryableStatusCodes().isEmpty());
        assertEquals(initialBackoff, retryPolicy.getInitialBackoff());
        assertEquals(maxBackoff, retryPolicy.getMaxBackoff());
    }

    @Test
    void testRetryPolicyWithNullStatusCodesThrowsException() {
        // Arrange
        int maxAttempts = 3;
        Duration initialBackoff = Duration.newBuilder().setSeconds(0).setNanos(25_000_000).build();
        Duration maxBackoff = Duration.newBuilder().setSeconds(0).setNanos(250_000_000).build();

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            new RetryPolicy(maxAttempts, null, initialBackoff, maxBackoff, null);
        });
    }

    @Test
    void testRetryPolicyWithNullInitialBackoffThrowsException() {
        // Arrange
        int maxAttempts = 3;
        List<Status.Code> statusCodes = Collections.singletonList(Status.Code.UNAVAILABLE);
        Duration maxBackoff = Duration.newBuilder().setSeconds(0).setNanos(250_000_000).build();

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            new RetryPolicy(maxAttempts, statusCodes, null, maxBackoff, null);
        });
    }

    @Test
    void testRetryPolicyWithNullMaxBackoffThrowsException() {
        // Arrange
        int maxAttempts = 3;
        List<Status.Code> statusCodes = Collections.singletonList(Status.Code.UNAVAILABLE);
        Duration initialBackoff = Duration.newBuilder().setSeconds(0).setNanos(25_000_000).build();

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            new RetryPolicy(maxAttempts, statusCodes, initialBackoff, null, null);
        });
    }

    @Test
    void testToString() {
        // Arrange
        int maxAttempts = 3;
        List<Status.Code> statusCodes = Arrays.asList(Status.Code.UNAVAILABLE, Status.Code.CANCELLED);
        Duration initialBackoff = Duration.newBuilder().setSeconds(0).setNanos(25_000_000).build();
        Duration maxBackoff = Duration.newBuilder().setSeconds(0).setNanos(250_000_000).build();

        RetryPolicy retryPolicy = new RetryPolicy(maxAttempts, statusCodes, initialBackoff, maxBackoff, null);

        // Act
        String result = retryPolicy.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    void testEquals() {
        // Arrange
        int maxAttempts = 3;
        List<Status.Code> statusCodes = Collections.singletonList(Status.Code.UNAVAILABLE);
        Duration initialBackoff = Duration.newBuilder().setSeconds(0).setNanos(25_000_000).build();
        Duration maxBackoff = Duration.newBuilder().setSeconds(0).setNanos(250_000_000).build();

        RetryPolicy policy1 = new RetryPolicy(maxAttempts, statusCodes, initialBackoff, maxBackoff, null);
        RetryPolicy policy2 = new RetryPolicy(maxAttempts, statusCodes, initialBackoff, maxBackoff, null);
        RetryPolicy policy3 = new RetryPolicy(5, statusCodes, initialBackoff, maxBackoff, null); // Different maxAttempts

        // Act & Assert
        assertEquals(policy1, policy2);
        assertNotEquals(policy1, policy3);
        assertNotEquals(policy1, null);
        assertNotEquals(policy1, "not a retry policy");
    }

    @Test
    void testHashCode() {
        // Arrange
        int maxAttempts = 3;
        List<Status.Code> statusCodes = Collections.singletonList(Status.Code.UNAVAILABLE);
        Duration initialBackoff = Duration.newBuilder().setSeconds(0).setNanos(25_000_000).build();
        Duration maxBackoff = Duration.newBuilder().setSeconds(0).setNanos(250_000_000).build();

        RetryPolicy policy1 = new RetryPolicy(maxAttempts, statusCodes, initialBackoff, maxBackoff, null);
        RetryPolicy policy2 = new RetryPolicy(maxAttempts, statusCodes, initialBackoff, maxBackoff, null);

        // Act & Assert
        assertEquals(policy1.hashCode(), policy2.hashCode());
    }

    @Test
    void testBoundaryValues() {
        // Test with boundary values
        int maxAttempts = Integer.MAX_VALUE;
        List<Status.Code> statusCodes = Collections.singletonList(Status.Code.UNAVAILABLE);
        Duration maxDuration = Duration.newBuilder().setSeconds(315576000000L).setNanos(999999999).build(); // Max duration

        RetryPolicy retryPolicy = new RetryPolicy(maxAttempts, statusCodes, maxDuration, maxDuration, maxDuration);

        assertEquals(Integer.MAX_VALUE, retryPolicy.getMaxAttempts());
        assertEquals(maxDuration, retryPolicy.getInitialBackoff());
        assertEquals(maxDuration, retryPolicy.getMaxBackoff());
        assertEquals(maxDuration, retryPolicy.getPerAttemptRecvTimeout());
    }

    @Test
    void testZeroMaxAttempts() {
        // Test with zero max attempts
        int maxAttempts = 0;
        List<Status.Code> statusCodes = Collections.singletonList(Status.Code.UNAVAILABLE);
        Duration initialBackoff = Duration.newBuilder().setSeconds(0).setNanos(25_000_000).build();
        Duration maxBackoff = Duration.newBuilder().setSeconds(0).setNanos(250_000_000).build();

        RetryPolicy retryPolicy = new RetryPolicy(maxAttempts, statusCodes, initialBackoff, maxBackoff, null);

        assertEquals(0, retryPolicy.getMaxAttempts());
    }

    @Test
    void testRetryableStatusCodesImmutability() {
        // Arrange
        List<Status.Code> originalCodes = Arrays.asList(Status.Code.UNAVAILABLE, Status.Code.CANCELLED);
        RetryPolicy retryPolicy = new RetryPolicy(3, originalCodes,
            Duration.newBuilder().setSeconds(0).setNanos(25_000_000).build(),
            Duration.newBuilder().setSeconds(0).setNanos(250_000_000).build(), null);

        // Act
        List<Status.Code> retrievedCodes = retryPolicy.getRetryableStatusCodes();

        // Assert
        assertEquals(originalCodes, retrievedCodes);

        // Verify that the returned list is immutable or a copy
        assertThrows(UnsupportedOperationException.class, () -> {
            retrievedCodes.clear();
        });
    }
}
