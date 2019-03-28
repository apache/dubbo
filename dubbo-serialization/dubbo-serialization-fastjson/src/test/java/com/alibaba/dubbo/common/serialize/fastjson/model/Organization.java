package com.alibaba.dubbo.common.serialize.fastjson.model;

/**
 * @author Born
 */
public class Organization<T> {

    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
