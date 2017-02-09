package com.alibaba.dubbo.common.serialize.support.jackson;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.common.serialize.support.json.JsonObjectInput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by wuyu on 2017/2/9.
 */
public class JacksonSerialization implements Serialization {

    @Override
    public byte getContentTypeId() {
        return 8;
    }

    @Override
    public String getContentType() {
        return "test/json";
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
        return new JacksonObjectOutput(output);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream input) throws IOException {
        return new JsonObjectInput(input);
    }
}
