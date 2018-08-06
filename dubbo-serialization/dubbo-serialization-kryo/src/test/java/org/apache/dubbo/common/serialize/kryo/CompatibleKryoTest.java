package org.apache.dubbo.common.serialize.kryo;

import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import org.apache.dubbo.common.serialize.kryo.serializer.CommonJavaSerializer;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.Assert.*;

public class CompatibleKryoTest {

    CompatibleKryo compatibleKryo = new CompatibleKryo();

    @Test
    public void getDefaultSerializer(){
        Serializer longSerializer = compatibleKryo.getDefaultSerializer(Long.class);
        assertEquals(CommonJavaSerializer.class, longSerializer.getClass());

        Serializer localDateTimeSerializer = compatibleKryo.getDefaultSerializer(LocalDateTime.class);
        assertEquals(CommonJavaSerializer.class, localDateTimeSerializer.getClass());

        Serializer dateSerializer = compatibleKryo.getDefaultSerializer(Date.class);
        assertEquals(DefaultSerializers.DateSerializer.class, dateSerializer.getClass());

        Serializer byteSerializer = compatibleKryo.getDefaultSerializer(Byte.class);
        assertEquals(CommonJavaSerializer.class, byteSerializer.getClass());

    }

    @Test
    public void newInstance() {
        Long longInstance = compatibleKryo.newInstance(Long.class);
        LocalDateTime localTimeInstance = compatibleKryo.newInstance(LocalDateTime.class);
        Date dateInstance = compatibleKryo.newInstance(Date.class);
        assertNotNull(localTimeInstance);
        assertNotNull(dateInstance);
        assertNotNull(longInstance);
    }
}