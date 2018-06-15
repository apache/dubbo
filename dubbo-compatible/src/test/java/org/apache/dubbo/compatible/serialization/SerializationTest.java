package org.apache.dubbo.compatible.serialization;

import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class SerializationTest {

    private MySerialization mySerialization;

    private MyObjectOutput myObjectOutput;
    private MyObjectInput myObjectInput;
    private ByteArrayOutputStream byteArrayOutputStream;
    private ByteArrayInputStream byteArrayInputStream;

    @Before
    public void setUp() throws Exception {
        this.mySerialization = new MySerialization();

        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.myObjectOutput = new MyObjectOutput(byteArrayOutputStream);
    }

    @Test
    public void testContentType() {
        assertThat(mySerialization.getContentType(), is("x-application/my"));
    }

    @Test
    public void testContentTypeId() {
        assertThat(mySerialization.getContentTypeId(), is((byte) 101));
    }

    @Test
    public void testObjectOutput() throws IOException {
        ObjectOutput objectOutput = mySerialization.serialize(null, mock(OutputStream.class));
        assertThat(objectOutput, Matchers.<ObjectOutput>instanceOf(MyObjectOutput.class));
    }

    @Test
    public void testObjectInput() throws IOException {
        ObjectInput objectInput = mySerialization.deserialize(null, mock(InputStream.class));
        assertThat(objectInput, Matchers.<ObjectInput>instanceOf(MyObjectInput.class));
    }

    @Test
    public void testWriteUTF() throws IOException {
        myObjectOutput.writeUTF("Pace");
        myObjectOutput.writeUTF("和平");
        myObjectOutput.writeUTF(" Мир");
        flushToInput();

        assertThat(myObjectInput.readUTF(), CoreMatchers.is("Pace"));
        assertThat(myObjectInput.readUTF(), CoreMatchers.is("和平"));
        assertThat(myObjectInput.readUTF(), CoreMatchers.is(" Мир"));
    }

    private void flushToInput() throws IOException {
        this.myObjectOutput.flushBuffer();
        this.byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.myObjectInput = new MyObjectInput(byteArrayInputStream);
    }
}