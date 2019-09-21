package org.apache.dubbo.common.serialize.msgpack;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.Constants;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * @author goodjava@163.com
 */
public class MsgPackSerialization implements Serialization {

    @Override
    public byte getContentTypeId() {
        return Constants.MSGPACK_SERIALIZATION_ID;
    }

    @Override
    public String getContentType() {
        return "msgpack";
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream output) {
        return new MsgpackObjectOutput(output);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream input) {
        return new MsgPackObjectInput(input);
    }

}
