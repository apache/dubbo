package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class OffsetDateTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -7823900532640515312L;

    /**
     * The local date-time.
     */
    private LocalDateTime dateTime;
    /**
     * The offset from UTC/Greenwich.
     */
    private ZoneOffset offset;

    public OffsetDateTimeHandle() {
    }

    public OffsetDateTimeHandle(OffsetDateTime offsetDateTime) {
        this.dateTime = offsetDateTime.toLocalDateTime();
        this.offset = offsetDateTime.getOffset();
    }

    private Object readResolve() {
        return OffsetDateTime.of(dateTime, offset);
    }
}
