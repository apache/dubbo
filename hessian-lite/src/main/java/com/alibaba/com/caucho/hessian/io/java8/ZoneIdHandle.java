package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.ZoneId;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class ZoneIdHandle implements HessianHandle, Serializable {
	
    private static final long serialVersionUID = 8789182864066905552L;

    private String zoneId;

    public ZoneIdHandle() {
    }

    public ZoneIdHandle(ZoneId zoneId) {
        this.zoneId = zoneId.getId();
    }

    private Object readResolve() {
        return ZoneId.of(zoneId);
    }
}
