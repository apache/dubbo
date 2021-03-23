package org.apache.dubbo.rpc.protocol.tri;

import java.io.InputStream;

public class Message {
    private final Object headers;
    private final InputStream is;

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
