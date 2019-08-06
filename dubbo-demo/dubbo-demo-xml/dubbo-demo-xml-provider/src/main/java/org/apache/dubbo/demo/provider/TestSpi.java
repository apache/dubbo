package org.apache.dubbo.demo.provider;

import org.apache.dubbo.demo.ISayName;

import java.util.ServiceLoader;

public class TestSpi {

    public static void main(String[] args) {
        ServiceLoader<ISayName> names = ServiceLoader.load(ISayName.class);
        for (ISayName name : names) {
            name.say();
        }
    }
}
