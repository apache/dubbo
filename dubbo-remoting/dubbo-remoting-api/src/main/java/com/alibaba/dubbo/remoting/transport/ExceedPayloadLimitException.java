package com.alibaba.dubbo.remoting.transport;

import java.io.IOException;

public class ExceedPayloadLimitException extends IOException {
    private static final long serialVersionUID = -1112322085391551410L;

    public ExceedPayloadLimitException(String message) {
        super(message);
    }
}
