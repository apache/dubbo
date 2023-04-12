package org.apache.dubbo.metrics.model.key;

@FunctionalInterface
public interface TpFunction<T, U, K, R> {
    R apply(T t, U u, K k);
}
