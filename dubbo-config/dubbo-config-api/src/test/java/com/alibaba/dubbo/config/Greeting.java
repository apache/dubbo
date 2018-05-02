package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.extension.SPI;

@SPI
interface Greeting {
    String hello();
}
