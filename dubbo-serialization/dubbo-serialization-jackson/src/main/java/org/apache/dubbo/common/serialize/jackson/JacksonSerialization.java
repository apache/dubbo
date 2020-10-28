package org.apache.dubbo.common.serialize.jackson;

import static org.apache.dubbo.common.serialize.Constants.JACKSON_SERIALIZATION_ID;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;

/**
 * Jackson serialization implementation
 * 
 * <pre>
 *     e.g. &lt;dubbo:protocol serialization="jackson" /&gt;
 * </pre>
 * 
 * @author Johnson.Jia
 */
public class JacksonSerialization implements Serialization {

    @Override
    public byte getContentTypeId() {
        return JACKSON_SERIALIZATION_ID;
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
        return new JacksonObjectOutput(output);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream input) throws IOException {
        return new JacksonObjectInput(input);
    }
}
