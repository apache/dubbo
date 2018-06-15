package org.apache.dubbo.compatible.serialization;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.serialize.Serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MySerialization implements Serialization {

    @Override
    public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
        return new MyObjectOutput(output);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream input) throws IOException {
        return new MyObjectInput(input);
    }

    @Override
    public byte getContentTypeId() {
        return 101;
    }

    @Override
    public String getContentType() {
        return "x-application/my";
    }
}
