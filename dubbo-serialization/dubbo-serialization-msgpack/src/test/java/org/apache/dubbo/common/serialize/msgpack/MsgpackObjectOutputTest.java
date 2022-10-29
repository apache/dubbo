/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.common.serialize.msgpack;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MsgpackObjectOutputTest {
    private MsgpackObjectOutput msgpackObjectOutput;
    private MsgpackObjectInput msgpackObjectInput;
    private ByteArrayOutputStream byteArrayOutputStream;
    private ByteArrayInputStream byteArrayInputStream;

    @BeforeEach
    public void setUp() throws Exception {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.msgpackObjectOutput = new MsgpackObjectOutput(byteArrayOutputStream);
    }

    @Test
    public void testWriteBool() throws IOException {
        this.msgpackObjectOutput.writeBool(true);
        this.flushToInput();

        assertThat(msgpackObjectInput.readBool(), is(true));
    }

    @Test
    public void testWriteShort() throws IOException {
        this.msgpackObjectOutput.writeShort((short) 2);
        this.flushToInput();

        assertThat(msgpackObjectInput.readShort(), is((short) 2));
    }

    @Test
    public void testWriteInt() throws IOException {
        this.msgpackObjectOutput.writeInt(1);
        this.flushToInput();

        assertThat(msgpackObjectInput.readInt(), is(1));
    }

    @Test
    public void testWriteLong() throws IOException {
        this.msgpackObjectOutput.writeLong(1000L);
        this.flushToInput();

        assertThat(msgpackObjectInput.readLong(), is(1000L));
    }

    @Test
    public void testWriteUTF() throws IOException {
        this.msgpackObjectOutput.writeUTF("Pace Hasîtî 和平 Мир");
        this.flushToInput();

        assertThat(msgpackObjectInput.readUTF(), is("Pace Hasîtî 和平 Мир"));
    }

    @Test
    public void testWriteUTF2() throws IOException {
        this.msgpackObjectOutput.writeUTF("a");
        this.msgpackObjectOutput.writeUTF("b");
        this.msgpackObjectOutput.writeUTF("c");
        this.flushToInput();
        assertThat(msgpackObjectInput.readUTF(), is("a"));
        assertThat(msgpackObjectInput.readUTF(), is("b"));
        assertThat(msgpackObjectInput.readUTF(), is("c"));
    }


    @Test
    public void testWriteThrowable() throws IOException, ClassNotFoundException {
        BizException throwable = new BizException("error");
        this.msgpackObjectOutput.writeThrowable(throwable);
        this.flushToInput();
        Throwable ex = msgpackObjectInput.readThrowable();
        assertThat(ex.getMessage(), is("error"));
        assertThat(ex.getClass(), is(BizException.class));
    }

    @Test
    public void testWriteFloat() throws IOException {
        this.msgpackObjectOutput.writeFloat(1.88f);
        this.flushToInput();

        assertThat(this.msgpackObjectInput.readFloat(), is(1.88f));
    }

    @Test
    public void testWriteDouble() throws IOException {
        this.msgpackObjectOutput.writeDouble(1.66d);
        this.flushToInput();

        assertThat(this.msgpackObjectInput.readDouble(), is(1.66d));
    }

    @Test
    public void testWriteBytes() throws IOException {
        this.msgpackObjectOutput.writeBytes("hello".getBytes());
        this.flushToInput();

        assertThat(this.msgpackObjectInput.readBytes(), is("hello".getBytes()));
    }

    @Test
    public void testWriteBytesWithSubLength() throws IOException {
        this.msgpackObjectOutput.writeBytes("hello".getBytes(), 2, 2);
        this.flushToInput();

        assertThat(this.msgpackObjectInput.readBytes(), is("ll".getBytes()));
    }

    @Test
    public void testWriteByte() throws IOException {
        this.msgpackObjectOutput.writeByte((byte) 123);
        this.flushToInput();

        assertThat(this.msgpackObjectInput.readByte(), is((byte) 123));
    }

    @Test
    public void testWriteObject() throws IOException, ClassNotFoundException {
        Image image = new Image("test.png", "logo", 300, 480, MsgpackObjectOutputTest.Image.Size.SMALL);
        this.msgpackObjectOutput.writeObject(image);
        this.flushToInput();
        Image readObjectForImage = msgpackObjectInput.readObject(Image.class);
        assertThat(readObjectForImage, not(nullValue()));
        assertThat(readObjectForImage, is(image));
    }

    private void flushToInput() throws IOException {
        this.msgpackObjectOutput.flushBuffer();
        this.byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.msgpackObjectInput = new MsgpackObjectInput(byteArrayInputStream);
    }


    public static class BizException extends RuntimeException {

        public BizException(String message) {
            super(message);
        }
        
    }

    public static class Image implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public String uri;
        public String title;  // Can be null
        public int width;
        public int height;
        public Image.Size size;

        public Image() {
        }

        public Image(String uri, String title, int width, int height, Image.Size size) {
            this.height = height;
            this.title = title;
            this.uri = uri;
            this.width = width;
            this.size = size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Image image = (Image) o;

            if (height != image.height) return false;
            if (width != image.width) return false;
            if (size != image.size) return false;
            if (title != null ? !title.equals(image.title) : image.title != null) return false;
            if (uri != null ? !uri.equals(image.uri) : image.uri != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = uri != null ? uri.hashCode() : 0;
            result = 31 * result + (title != null ? title.hashCode() : 0);
            result = 31 * result + width;
            result = 31 * result + height;
            result = 31 * result + (size != null ? size.hashCode() : 0);
            return result;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[Image ");
            sb.append("uri=").append(uri);
            sb.append(", title=").append(title);
            sb.append(", width=").append(width);
            sb.append(", height=").append(height);
            sb.append(", size=").append(size);
            sb.append("]");
            return sb.toString();
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public Image.Size getSize() {
            return size;
        }

        public void setSize(Image.Size size) {
            this.size = size;
        }

        public enum Size {
            SMALL, LARGE
        }
    }
}
