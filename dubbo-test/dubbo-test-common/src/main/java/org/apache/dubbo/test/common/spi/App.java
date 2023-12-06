package org.apache.dubbo.test.common.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

public class App {
    public static void main(String[] args) {
        ServiceLoader<Log> serviceLoader = ServiceLoader.load(Log.class);
        Iterator<Log> iterator = serviceLoader.iterator();
        if (iterator.hasNext()) {
            Log log = iterator.next();
            log.log("jdk spi");
        }
    }
}
