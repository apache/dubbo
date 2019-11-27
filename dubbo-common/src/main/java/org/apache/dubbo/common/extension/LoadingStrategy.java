package org.apache.dubbo.common.extension;

public interface LoadingStrategy {
    String directory();

    default boolean preferExtensionClassLoader() {
        return false;
    }

    default String[] excludedPackages() {
        return null;
    }
}
