package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.Instant;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class InstantHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -4367309317780077156L;

    private long seconds;

    private int nanos;

    public InstantHandle() {
    }

    public InstantHandle(Instant instant) {
    	this.seconds = instant.getEpochSecond();
    	this.nanos = instant.getNano();
    }

    private Object readResolve() {
        return Instant.ofEpochSecond(seconds, nanos);
    }
}
