package com.alibaba.dubbo.rpc.protocol.thrift.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class InputStreamWrapper extends InputStream {

    private InputStream is;

    public InputStreamWrapper(InputStream is) {
        if (is == null) {
            throw new NullPointerException("is == null");
        }
        this.is = is;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (is.available() >= b.length) {
            return is.read(b);
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (is.available() >= len) {
            return is.read(b, off, len);
        } else {
            return -1;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        return is.skip(n);
    }

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public void mark(int readlimit) {
        is.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        is.reset();
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }

    @Override
    public int read() throws IOException {
        if (is.available() >= 1) {
            return is.read();
        }
        return -1;
    }
}
