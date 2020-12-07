package org.apache.dubbo.remoting.transport.netty4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.dubbo.remoting.transport.netty4.grpc.GrpcElf;

import static io.netty.util.CharsetUtil.US_ASCII;
import static io.netty.util.CharsetUtil.UTF_8;

/**
 * COPY FROM GRPC-JAVA
 */
public class Status {

    // Create the canonical list of Status instances indexed by their code values.
    private static final List<Status> STATUS_LIST = buildStatusList();
    /**
     * The operation completed successfully.
     */
    public static final Status OK = Code.OK.toStatus();
    /**
     * The operation was cancelled (typically by the caller).
     */
    public static final Status CANCELLED = Code.CANCELLED.toStatus();

    // A pseudo-enum of Status instances mapped 1:1 with values in Code. This simplifies construction
    // patterns for derived instances of Status.
    /**
     * Unknown error. See {@link Code#UNKNOWN}.
     */
    public static final Status UNKNOWN = Code.UNKNOWN.toStatus();
    /**
     * Client specified an invalid argument. See {@link Code#INVALID_ARGUMENT}.
     */
    public static final Status INVALID_ARGUMENT = Code.INVALID_ARGUMENT.toStatus();
    /**
     * Deadline expired before operation could complete. See {@link Code#DEADLINE_EXCEEDED}.
     */
    public static final Status DEADLINE_EXCEEDED = Code.DEADLINE_EXCEEDED.toStatus();
    /**
     * Some requested entity (e.g., file or directory) was not found.
     */
    public static final Status NOT_FOUND = Code.NOT_FOUND.toStatus();
    /**
     * Some entity that we attempted to create (e.g., file or directory) already exists.
     */
    public static final Status ALREADY_EXISTS = Code.ALREADY_EXISTS.toStatus();
    /**
     * The caller does not have permission to execute the specified operation. See {@link
     * Code#PERMISSION_DENIED}.
     */
    public static final Status PERMISSION_DENIED = Code.PERMISSION_DENIED.toStatus();
    /**
     * The request does not have valid authentication credentials for the operation.
     */
    public static final Status UNAUTHENTICATED = Code.UNAUTHENTICATED.toStatus();
    /**
     * Some resource has been exhausted, perhaps a per-user quota, or perhaps the entire file system
     * is out of space.
     */
    public static final Status RESOURCE_EXHAUSTED = Code.RESOURCE_EXHAUSTED.toStatus();
    /**
     * Operation was rejected because the system is not in a state required for the operation's
     * execution. See {@link Code#FAILED_PRECONDITION}.
     */
    public static final Status FAILED_PRECONDITION =
            Code.FAILED_PRECONDITION.toStatus();
    /**
     * The operation was aborted, typically due to a concurrency issue like sequencer check failures,
     * transaction aborts, etc. See {@link Code#ABORTED}.
     */
    public static final Status ABORTED = Code.ABORTED.toStatus();
    /**
     * Operation was attempted past the valid range. See {@link Code#OUT_OF_RANGE}.
     */
    public static final Status OUT_OF_RANGE = Code.OUT_OF_RANGE.toStatus();
    /**
     * Operation is not implemented or not supported/enabled in this service.
     */
    public static final Status UNIMPLEMENTED = Code.UNIMPLEMENTED.toStatus();
    /**
     * Internal errors. See {@link Code#INTERNAL}.
     */
    public static final Status INTERNAL = Code.INTERNAL.toStatus();
    /**
     * The service is currently unavailable. See {@link Code#UNAVAILABLE}.
     */
    public static final Status UNAVAILABLE = Code.UNAVAILABLE.toStatus();
    /**
     * Unrecoverable data loss or corruption.
     */
    public static final Status DATA_LOSS = Code.DATA_LOSS.toStatus();
    private final Code code;
    private final String description;
    private final Throwable cause;

    private Status(Code code) {
        this(code, null, null);
    }

    private Status(Code code, String description, Throwable cause) {
        this.code = code;
        this.description = description;
        this.cause = cause;
    }

    private static List<Status> buildStatusList() {
        TreeMap<Integer, Status> canonicalizer = new TreeMap<>();
        for (Code code : Code.values()) {
            Status replaced = canonicalizer.put(code.value(), new Status(code));
            if (replaced != null) {
                throw new IllegalStateException("Code value duplication between "
                        + replaced.getCode().name() + " & " + code.name());
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(canonicalizer.values()));
    }

    /**
     * Return a {@link Status} given a canonical error {@link Code} value.
     */
    public static Status fromCodeValue(int codeValue) {
        if (codeValue < 0 || codeValue > STATUS_LIST.size()) {
            return UNKNOWN.withDescription("Unknown code " + codeValue);
        } else {
            return STATUS_LIST.get(codeValue);
        }

    }

    private static Status fromCodeValue(byte[] asciiCodeValue) {
        if (asciiCodeValue.length == 1 && asciiCodeValue[0] == '0') {
            return Status.OK;
        }
        return fromCodeValueSlow(asciiCodeValue);
    }

    public static Status fromThrowable(Throwable t) {
        //Throwable cause = checkNotNull(t, "t");
        //while (cause != null) {
        //    if (cause instanceof StatusException) {
        //        return ((StatusException) cause).getStatus();
        //    } else if (cause instanceof StatusRuntimeException) {
        //        return ((StatusRuntimeException) cause).getStatus();
        //    }
        //    cause = cause.getCause();
        //}
        // Couldn't find a cause with a Status
        return UNKNOWN.withCause(t);
    }

    public static String formatThrowableMessage(Status status) {
        if (status.description == null) {
            return status.code.toString();
        } else {
            return status.code + ": " + status.description;
        }
    }

    @SuppressWarnings("fallthrough")
    private static Status fromCodeValueSlow(byte[] asciiCodeValue) {
        int index = 0;
        int codeValue = 0;
        switch (asciiCodeValue.length) {
            case 2:
                if (asciiCodeValue[index] < '0' || asciiCodeValue[index] > '9') {
                    break;
                }
                codeValue += (asciiCodeValue[index++] - '0') * 10;
                // fall through
            case 1:
                if (asciiCodeValue[index] < '0' || asciiCodeValue[index] > '9') {
                    break;
                }
                codeValue += asciiCodeValue[index] - '0';
                if (codeValue < STATUS_LIST.size()) {
                    return STATUS_LIST.get(codeValue);
                }
                break;
            default:
                break;
        }
        return UNKNOWN.withDescription("Unknown code " + new String(asciiCodeValue, US_ASCII));
    }

    /**
     * Return a {@link Status} given a canonical error {@link Code} object.
     */
    public static Status fromCode(Code code) {
        return code.toStatus();
    }

    public static Status statusFromGoAway(long errorCode, byte[] debugData) {
        Status status = GrpcElf.Http2Error.statusForCode((int) errorCode)
                .augmentDescription("Received Goaway");
        if (debugData != null && debugData.length > 0) {
            // If a debug message was provided, use it.
            String msg = new String(debugData, UTF_8);
            status = status.augmentDescription(msg);
        }
        return status;
    }

    /**
     * Convert this {@link Status} to a {@link RuntimeException}. Use {@link #fromThrowable}
     * to recover this {@link Status} instance when the returned exception is in the causal chain.
     */
    //public StatusRuntimeException asRuntimeException() {
    //    return new StatusRuntimeException(this);
    //}

    /**
     * Convert this {@link Status} to an {@link Exception}. Use {@link #fromThrowable}
     * to recover this {@link Status} instance when the returned exception is in the causal chain.
     */
    //public StatusException asException() {
    //    return new StatusException(this);
    //}

    /**
     * Create a derived instance of {@link Status} with the given description.  Leading and trailing
     * whitespace may be removed; this may change in the future.
     */
    public Status withDescription(String description) {
        if (Objects.equals(this.description, description)) {
            return this;
        }
        return new Status(this.code, description, this.cause);
    }

    /**
     * Create a derived instance of {@link Status} with the given cause.
     * However, the cause is not transmitted from server to client.
     */
    public Status withCause(Throwable cause) {
        if (Objects.equals(this.cause, cause)) {
            return this;
        }
        return new Status(this.code, this.description, cause);
    }

    /**
     * The canonical status code.
     */
    public Code getCode() {
        return code;
    }

    /**
     * A description of this status for human consumption.
     */
    public String getDescription() {
        return description;
    }

    /**
     * The underlying cause of an error.
     * Note that the cause is not transmitted from server to client.
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Is this status OK, i.e., not an error.
     */
    public boolean isOk() {
        return Code.OK == code;
    }

    /**
     * Create a derived instance of {@link Status} augmenting the current description with
     * additional detail.  Leading and trailing whitespace may be removed; this may change in the
     * future.
     */
    public Status augmentDescription(String additionalDetail) {
        if (additionalDetail == null) {
            return this;
        } else if (this.description == null) {
            return new Status(this.code, additionalDetail, this.cause);
        } else {
            return new Status(this.code, this.description + "\n" + additionalDetail, this.cause);
        }
    }

    /**
     * The set of canonical status codes. If new codes are added over time they must choose
     * a numerical value that does not collide with any previously used value.
     */
    public enum Code {
        /**
         * The operation completed successfully.
         */
        OK(0),

        /**
         * The operation was cancelled (typically by the caller).
         */
        CANCELLED(1),

        /**
         * Unknown error.  An example of where this error may be returned is
         * if a Status value received from another address space belongs to
         * an error-space that is not known in this address space.  Also
         * errors raised by APIs that do not return enough error information
         * may be converted to this error.
         */
        UNKNOWN(2),

        /**
         * Client specified an invalid argument.  Note that this differs
         * from FAILED_PRECONDITION.  INVALID_ARGUMENT indicates arguments
         * that are problematic regardless of the state of the system
         * (e.g., a malformed file name).
         */
        INVALID_ARGUMENT(3),

        /**
         * Deadline expired before operation could complete.  For operations
         * that change the state of the system, this error may be returned
         * even if the operation has completed successfully.  For example, a
         * successful response from a server could have been delayed long
         * enough for the deadline to expire.
         */
        DEADLINE_EXCEEDED(4),

        /**
         * Some requested entity (e.g., file or directory) was not found.
         */
        NOT_FOUND(5),

        /**
         * Some entity that we attempted to create (e.g., file or directory) already exists.
         */
        ALREADY_EXISTS(6),

        /**
         * The caller does not have permission to execute the specified
         * operation.  PERMISSION_DENIED must not be used for rejections
         * caused by exhausting some resource (use RESOURCE_EXHAUSTED
         * instead for those errors).  PERMISSION_DENIED must not be
         * used if the caller cannot be identified (use UNAUTHENTICATED
         * instead for those errors).
         */
        PERMISSION_DENIED(7),

        /**
         * Some resource has been exhausted, perhaps a per-user quota, or
         * perhaps the entire file system is out of space.
         */
        RESOURCE_EXHAUSTED(8),

        /**
         * Operation was rejected because the system is not in a state
         * required for the operation's execution.  For example, directory
         * to be deleted may be non-empty, an rmdir operation is applied to
         * a non-directory, etc.
         *
         * <p>A litmus test that may help a service implementor in deciding
         * between FAILED_PRECONDITION, ABORTED, and UNAVAILABLE:
         * (a) Use UNAVAILABLE if the client can retry just the failing call.
         * (b) Use ABORTED if the client should retry at a higher-level
         * (e.g., restarting a read-modify-write sequence).
         * (c) Use FAILED_PRECONDITION if the client should not retry until
         * the system state has been explicitly fixed.  E.g., if an "rmdir"
         * fails because the directory is non-empty, FAILED_PRECONDITION
         * should be returned since the client should not retry unless
         * they have first fixed up the directory by deleting files from it.
         */
        FAILED_PRECONDITION(9),

        /**
         * The operation was aborted, typically due to a concurrency issue
         * like sequencer check failures, transaction aborts, etc.
         *
         * <p>See litmus test above for deciding between FAILED_PRECONDITION,
         * ABORTED, and UNAVAILABLE.
         */
        ABORTED(10),

        /**
         * Operation was attempted past the valid range.  E.g., seeking or
         * reading past end of file.
         *
         * <p>Unlike INVALID_ARGUMENT, this error indicates a problem that may
         * be fixed if the system state changes. For example, a 32-bit file
         * system will generate INVALID_ARGUMENT if asked to read at an
         * offset that is not in the range [0,2^32-1], but it will generate
         * OUT_OF_RANGE if asked to read from an offset past the current
         * file size.
         *
         * <p>There is a fair bit of overlap between FAILED_PRECONDITION and OUT_OF_RANGE.
         * We recommend using OUT_OF_RANGE (the more specific error) when it applies
         * so that callers who are iterating through
         * a space can easily look for an OUT_OF_RANGE error to detect when they are done.
         */
        OUT_OF_RANGE(11),

        /**
         * Operation is not implemented or not supported/enabled in this service.
         */
        UNIMPLEMENTED(12),

        /**
         * Internal errors.  Means some invariants expected by underlying
         * system has been broken.  If you see one of these errors,
         * something is very broken.
         */
        INTERNAL(13),

        /**
         * The service is currently unavailable.  This is a most likely a
         * transient condition and may be corrected by retrying with
         * a backoff. Note that it is not always safe to retry
         * non-idempotent operations.
         *
         * <p>See litmus test above for deciding between FAILED_PRECONDITION,
         * ABORTED, and UNAVAILABLE.
         */
        UNAVAILABLE(14),

        /**
         * Unrecoverable data loss or corruption.
         */
        DATA_LOSS(15),

        /**
         * The request does not have valid authentication credentials for the
         * operation.
         */
        UNAUTHENTICATED(16);

        private final int value;
        @SuppressWarnings("ImmutableEnumChecker") // we make sure the byte[] can't be modified
        private final byte[] valueAscii;

        Code(int value) {
            this.value = value;
            this.valueAscii = Integer.toString(value).getBytes(US_ASCII);
        }

        /**
         * The numerical value of the code.
         */
        public int value() {
            return value;
        }

        /**
         * Returns a {@link Status} object corresponding to this status code.
         */
        public Status toStatus() {
            return STATUS_LIST.get(value);
        }

        private byte[] valueAscii() {
            return valueAscii;
        }
    }

}
