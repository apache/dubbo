package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class OffsetTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -3269846941421652860L;

    private LocalTime localTime;

    private ZoneOffset zoneOffset;

    public OffsetTimeHandle() {
    }

    public OffsetTimeHandle(OffsetTime offsetTime) {
    	this.zoneOffset = offsetTime.getOffset();
        this.localTime = offsetTime.toLocalTime();
    }

    private Object readResolve() {
        return OffsetTime.of(localTime, zoneOffset);
    }
}
