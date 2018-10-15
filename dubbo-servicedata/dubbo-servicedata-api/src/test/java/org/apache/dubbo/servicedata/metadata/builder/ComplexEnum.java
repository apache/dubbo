package org.apache.dubbo.servicedata.metadata.builder;

/**
 *  2018/9/28
 */
public enum ComplexEnum {
    F(11, "t1"), S(22, "t2"), T(33, "t1");

    ComplexEnum(int code, String message) {
        this.code = code;
        this.msg = message;
    }

    private int code;
    private String msg;
}
