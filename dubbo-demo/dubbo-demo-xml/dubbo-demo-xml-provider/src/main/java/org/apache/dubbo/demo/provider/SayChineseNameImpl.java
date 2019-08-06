package org.apache.dubbo.demo.provider;

import org.apache.dubbo.demo.ISayName;

public class SayChineseNameImpl implements ISayName {
    @Override
    public void say() {
        System.out.printf("测试");
    }
}
