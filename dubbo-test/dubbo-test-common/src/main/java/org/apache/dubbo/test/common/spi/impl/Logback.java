package org.apache.dubbo.test.common.spi.impl;

import org.apache.dubbo.test.common.spi.Log;

public class Logback implements Log {
    @Override
    public void log(String info) {
        System.out.println("logback: " + info);
    }
}
