package org.apache.dubbo.common.serialize.protobuf;

/**
 *
 */
public class Wrapper<T> {
    private T data;

    Wrapper(T data) {
        this.data = data;
    }

    Object getData() {
        return data;
    }
}
