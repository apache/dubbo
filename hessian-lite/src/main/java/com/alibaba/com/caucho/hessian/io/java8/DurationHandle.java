package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.Duration;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class DurationHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -4367309317780077156L;

    private long seconds;

    private int nanos;

    public DurationHandle() {
    }

    public DurationHandle(Duration duration) {
        this.seconds = duration.getSeconds();
        this.nanos = duration.getNano();
    }

    private Object readResolve() {
        return Duration.ofSeconds(seconds, nanos);
    }
}
