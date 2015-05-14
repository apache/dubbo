package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class ZonedDateTimeHandle implements HessianHandle, Serializable {
	
	private static final long serialVersionUID = -6933460123278647569L;

	/**
     * The local date-time.
     */
    private LocalDateTime dateTime;
    /**
     * The offset from UTC/Greenwich.
     */
    private ZoneOffset offset;
    /**
     * The time-zoneId.
     */
    private String zoneId;


    public ZonedDateTimeHandle() {
    }

    public ZonedDateTimeHandle(ZonedDateTime zonedDateTime) {
        this.dateTime = zonedDateTime.toLocalDateTime();
        this.offset = zonedDateTime.getOffset();
        if(zonedDateTime.getZone() != null) {
            this.zoneId = zonedDateTime.getZone().getId();
        }
    }

    private Object readResolve() {
        return ZonedDateTime.ofLocal(dateTime, ZoneId.of(zoneId), offset);
    }
}
