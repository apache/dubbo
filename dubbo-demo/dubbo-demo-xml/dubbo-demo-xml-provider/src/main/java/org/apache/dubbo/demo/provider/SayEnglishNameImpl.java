package org.apache.dubbo.demo.provider;

import org.apache.dubbo.demo.ISayName;

public class SayEnglishNameImpl implements ISayName {

    @Override
    public void say() {
        System.out.printf("toby");
    }
}
