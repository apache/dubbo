package org.apache.dubbo.rpc.service;

public class DemoException extends Exception {

    private static final long serialVersionUID = -8213943026163641747L;

    public DemoException() {
        super();
    }

    public DemoException(String message, Throwable cause) {
        super(message, cause);
    }

    public DemoException(String message) {
        super(message);
    }

    public DemoException(Throwable cause) {
        super(cause);
    }

}

