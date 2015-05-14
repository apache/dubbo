package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.ZoneOffset;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class ZoneOffsetHandle implements HessianHandle, Serializable {
	
    private static final long serialVersionUID = 8841589723587858789L;

    private int seconds;

    public ZoneOffsetHandle() {
    }

    public ZoneOffsetHandle(ZoneOffset zoneOffset) {
        this.seconds = zoneOffset.getTotalSeconds();
    }

    private Object readResolve() {
        return ZoneOffset.ofTotalSeconds(seconds);
    }
}
