package org.apache.dubbo.common.serialize.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import org.apache.dubbo.common.serialize.kryo.CompatibleKryo;

public class CommonJavaSerializer extends JavaSerializer {
    private Kryo kryo = new CompatibleKryo();

    @Override
    public void write(Kryo kryo, Output output, Object object) {
        super.write(this.kryo, output, object);
    }

    @Override
    public Object read(Kryo kryo, Input input, Class type) {
        return super.read(this.kryo, input, type);
    }
}
