package org.apache.dubbo.common.serialize.avro;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class AvroSerializationTest {
	 private AvroSerialization avroSerialization;

	    @Before
	    public void setUp() {
	        this.avroSerialization = new AvroSerialization();
	    }

	    @Test
	    public void testContentType() {
	        assertThat(avroSerialization.getContentType(), is("avro/binary"));
	    }

	    @Test
	    public void testContentTypeId() {
	        assertThat(avroSerialization.getContentTypeId(), is((byte) 10));
	    }

	    @Test
	    public void testObjectOutput() throws IOException {
	        ObjectOutput objectOutput = avroSerialization.serialize(null, mock(OutputStream.class));
	        assertThat(objectOutput, Matchers.<ObjectOutput>instanceOf(AvroObjectOutput.class));
	    }

	    @Test
	    public void testObjectInput() throws IOException {
	        ObjectInput objectInput = avroSerialization.deserialize(null, mock(InputStream.class));
	        assertThat(objectInput, Matchers.<ObjectInput>instanceOf(AvroObjectInput.class));
	    }
}
