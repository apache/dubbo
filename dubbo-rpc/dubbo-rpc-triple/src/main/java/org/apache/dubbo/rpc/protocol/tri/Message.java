package org.apache.dubbo.rpc.protocol.tri;

import java.io.InputStream;

public class Message {
    private Object headers;
    private InputStream is;

    public Message(Object headers, InputStream is) {
        this.headers = headers;
        this.is = is;
    }

    public Object getHeaders() {
        return headers;
    }

    public InputStream getIs() {
        return is;
    }
}
