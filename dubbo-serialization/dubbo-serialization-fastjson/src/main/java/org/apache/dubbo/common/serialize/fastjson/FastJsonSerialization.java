package org.apache.dubbo.common.serialize.fastjson;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.apache.dubbo.common.serialize.Constants.FASTJSON_SERIALIZATION_ID;

/**
 * @author owen.cai
 * @create_date 2022/5/19
 * @alter_author
 * @alter_date
 */
public class FastJsonSerialization implements Serialization {
    @Override
    public byte getContentTypeId() {
        return FASTJSON_SERIALIZATION_ID;
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
        return new FastJsonObjectOutput(output);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream input) throws IOException {
        return new FastJsonObjectInput(input);
    }
}
