package org.apache.dubbo.rpc.protocol.grpc;

import io.netty.util.AsciiString;

public class GrpcElf {
    public static final AsciiString GRPC_STATUS = AsciiString.cached("grpc-status");
    public static final AsciiString GRPC_MESSAGE = AsciiString.cached("grpc-message");
    public static final AsciiString GRPC_ENCODING = AsciiString.cached("grpc-encoding");
    public static final AsciiString GRPC_TIMEOUT = AsciiString.cached("grpc-timeout");
    public static final AsciiString GRPC_ACCEPT_ENCODING = AsciiString.cached("grpc-accept-encoding");

    public static final AsciiString APPLICATION_GRPC = AsciiString.cached("application/grpc");
    public static final AsciiString TEXT_MIME = AsciiString.cached("text/plain; encoding=utf-8");
    public static final AsciiString GRPC_JSON = AsciiString.cached("application/grpc+json");
    public static final AsciiString GRPC_PROTO = AsciiString.cached("application/grpc+proto");


    /**
     * Indicates whether or not the given value is a valid gRPC content-type.
     */
    public static boolean isGrpcContentType(CharSequence contentType) {
        if (contentType == null) {
            return false;
        }

        if (APPLICATION_GRPC.length() > contentType.length()) {
            return false;
        }

        if (APPLICATION_GRPC.contentEquals(contentType)) {
            return true;
        }
        if (!AsciiString.of(contentType).startsWith(APPLICATION_GRPC)) {
            // Not a gRPC content-type.
            return false;
        }

        if (contentType.length() == APPLICATION_GRPC.length()) {
            // The strings match exactly.
            return true;
        }

        // The contentType matches, but is longer than the expected string.
        // We need to support variations on the content-type (e.g. +proto, +json) as defined by the
        // gRPC wire spec.
        char nextChar = contentType.charAt(APPLICATION_GRPC.length());
        return nextChar == '+' || nextChar == ';';
    }

    /**
     * All error codes identified by the HTTP/2 spec. Used in GOAWAY and RST_STREAM frames.
     */
    //public enum Http2Error {
    //    /**
    //     * Servers implementing a graceful shutdown of the connection will send {@code GOAWAY} with
    //     * {@code NO_ERROR}. In this case it is important to indicate to the application that the
    //     * request should be retried (i.e. {@link Status#UNAVAILABLE}).
    //     */
    //    NO_ERROR(0x0, Status.UNAVAILABLE),
    //    PROTOCOL_ERROR(0x1, Status.INTERNAL),
    //    INTERNAL_ERROR(0x2, Status.INTERNAL),
    //    FLOW_CONTROL_ERROR(0x3, Status.INTERNAL),
    //    SETTINGS_TIMEOUT(0x4, Status.INTERNAL),
    //    STREAM_CLOSED(0x5, Status.INTERNAL),
    //    FRAME_SIZE_ERROR(0x6, Status.INTERNAL),
    //    REFUSED_STREAM(0x7, Status.UNAVAILABLE),
    //    CANCEL(0x8, Status.CANCELLED),
    //    COMPRESSION_ERROR(0x9, Status.INTERNAL),
    //    CONNECT_ERROR(0xA, Status.INTERNAL),
    //    ENHANCE_YOUR_CALM(0xB, Status.RESOURCE_EXHAUSTED.withDescription("Bandwidth exhausted")),
    //    INADEQUATE_SECURITY(0xC, Status.PERMISSION_DENIED.withDescription("Permission denied as "
    //            + "protocol is not secure enough to call")),
    //    HTTP_1_1_REQUIRED(0xD, Status.UNKNOWN);
    //
    //    // Populate a mapping of code to enum value for quick look-up.
    //    private static final Http2Error[] codeMap = buildHttp2CodeMap();
    //    private final int code;
    //    // Status is not guaranteed to be deeply immutable. Don't care though, since that's only true
    //    // when there are exceptions in the Status, which is not true here.
    //    @SuppressWarnings("ImmutableEnumChecker")
    //    private final Status status;
    //
    //    Http2Error(int code, Status status) {
    //        this.code = code;
    //        this.status = status.augmentDescription("HTTP/2 error code: " + this.name());
    //    }
    //
    //    private static Http2Error[] buildHttp2CodeMap() {
    //        Http2Error[] errors = Http2Error.values();
    //        int size = (int) errors[errors.length - 1].code() + 1;
    //        Http2Error[] http2CodeMap = new Http2Error[size];
    //        for (Http2Error error : errors) {
    //            int index = (int) error.code();
    //            http2CodeMap[index] = error;
    //        }
    //        return http2CodeMap;
    //    }
    //
    //    /**
    //     * Looks up the HTTP/2 error code enum value for the specified code.
    //     *
    //     * @param code an HTTP/2 error code value.
    //     * @return the HTTP/2 error code enum or {@code null} if not found.
    //     */
    //    public static Http2Error forCode(long code) {
    //        if (code >= codeMap.length || code < 0) {
    //            return null;
    //        }
    //        return codeMap[(int) code];
    //    }
    //
    //    /**
    //     * Looks up the {@link Status} from the given HTTP/2 error code. This is preferred over {@code
    //     * forCode(code).status()}, to more easily conform to HTTP/2:
    //     *
    //     * <blockquote>Unknown or unsupported error codes MUST NOT trigger any special behavior.
    //     * These MAY be treated by an implementation as being equivalent to INTERNAL_ERROR.</blockquote>
    //     *
    //     * @param code the HTTP/2 error code.
    //     * @return a {@link Status} representing the given error.
    //     */
    //    public static Status statusForCode(long code) {
    //        Http2Error error = forCode(code);
    //        if (error == null) {
    //            // This "forgets" the message of INTERNAL_ERROR while keeping the same status code.
    //            Status.Code statusCode = INTERNAL_ERROR.status().getCode();
    //            return Status.fromCodeValue(statusCode.value())
    //                    .withDescription("Unrecognized HTTP/2 error code: " + code);
    //        }
    //
    //        return error.status();
    //    }
    //
    //    /**
    //     * Gets the code for this error used on the wire.
    //     */
    //    public long code() {
    //        return code;
    //    }
    //
    //    /**
    //     * Gets the {@link Status} associated with this HTTP/2 code.
    //     */
    //    public Status status() {
    //        return status;
    //    }
    //}
}
