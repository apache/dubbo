package org.apache.dubbo.common.serialize.protostuff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProtostuffObjectOutputTest {

    private ByteArrayOutputStream byteArrayOutputStream;
    private ProtostuffObjectOutput protostuffObjectOutput;
    private ProtostuffObjectInput protostuffObjectInput;
    private ByteArrayInputStream byteArrayInputStream;

    @BeforeEach
    public void setUp() throws Exception {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.protostuffObjectOutput = new ProtostuffObjectOutput(byteArrayOutputStream);
    }

    @Test
    public void testWriteObjectNull() throws IOException, ClassNotFoundException {
        this.protostuffObjectOutput.writeObject(null);
        this.flushToInput();

        assertThat(protostuffObjectInput.readObject(), nullValue());
    }

    private void flushToInput() throws IOException {
        this.protostuffObjectOutput.flushBuffer();
        this.byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.protostuffObjectInput = new ProtostuffObjectInput(byteArrayInputStream);
    }
}
