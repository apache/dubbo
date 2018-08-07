package org.apache.dubbo.common.serialize.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import org.apache.dubbo.common.serialize.kryo.serializer.CommonJavaSerializer;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExtensionKryoTest {

    private Kryo extensionKryo = new ExtensionKryo();

    @Test
    public void getDefaultSerializer(){
        Serializer longSerializer = extensionKryo.getDefaultSerializer(Long.class);
        assertEquals(CommonJavaSerializer.class, longSerializer.getClass());

        Serializer localDateTimeSerializer = extensionKryo.getDefaultSerializer(LocalDateTime.class);
        assertEquals(CommonJavaSerializer.class, localDateTimeSerializer.getClass());

        Serializer dateSerializer = extensionKryo.getDefaultSerializer(Date.class);
        assertEquals(DefaultSerializers.DateSerializer.class, dateSerializer.getClass());

        Serializer byteSerializer = extensionKryo.getDefaultSerializer(Byte.class);
        assertEquals(CommonJavaSerializer.class, byteSerializer.getClass());

    }

    @Test
    public void newInstance() {
        Long longInstance = extensionKryo.newInstance(Long.class);
        LocalDateTime localTimeInstance = extensionKryo.newInstance(LocalDateTime.class);
        Date dateInstance = extensionKryo.newInstance(Date.class);
        assertNotNull(localTimeInstance);
        assertNotNull(dateInstance);
        assertNotNull(longInstance);
    }
}