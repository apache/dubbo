package org.apache.dubbo.rpc.protocol.tri;

public class ClassLoadUtil {
    public static void switchContextLoader(ClassLoader loader) {
        try {
            if (loader != null && loader != Thread.currentThread().getContextClassLoader()) {
                Thread.currentThread().setContextClassLoader(loader);
            }
        } catch (SecurityException e) {
            // ignore , ForkJoinPool & jdk8 & securityManager will cause this
        }
    }
}
