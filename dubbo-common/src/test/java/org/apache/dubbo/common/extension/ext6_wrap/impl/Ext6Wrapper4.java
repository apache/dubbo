package org.apache.dubbo.common.extension.ext6_wrap.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Wrapper;
import org.apache.dubbo.common.extension.ext6_wrap.WrappedExt;
import org.apache.dubbo.common.extension.ext6_wrap.WrappedExtWrapper;

import java.util.concurrent.atomic.AtomicInteger;

@Wrapper(mismatches = {"impl1", "impl2"}, order = 4)
public class Ext6Wrapper4 implements WrappedExt, WrappedExtWrapper {
    public static AtomicInteger echoCount = new AtomicInteger();
    WrappedExt origin;

    public Ext6Wrapper4(WrappedExt origin) {
        this.origin = origin;
    }

    public String echo(URL url, String s) {
        echoCount.incrementAndGet();
        return origin.echo(url, s);
    }

    public WrappedExt getOrigin() {
        return origin;
    }
}
