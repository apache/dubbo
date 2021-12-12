package org.apache.dubbo.common.extension.ext6_wrap.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ext6_wrap.WrappedExt;

public class Ext6Impl3 implements WrappedExt {

    @Override
    public String echo(URL url, String s) {
        return "Ext6Impl3-echo";
    }
}
