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
package org.apache.dubbo.common.serialize.fastjson;

import com.alibaba.fastjson.parser.ParserConfig;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.remoting.RemotingException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class FastJsonSerializationTest {
    private FastJsonSerialization fastJsonSerialization;

    @BeforeEach
    public void setUp() {
        this.fastJsonSerialization = new FastJsonSerialization();
    }

    @Test
    public void testContentType() {
        assertThat(fastJsonSerialization.getContentType(), is("text/json"));
    }

    @Test
    public void testContentTypeId() {
        assertThat(fastJsonSerialization.getContentTypeId(), is((byte) 6));
    }

    @Test
    public void testObjectOutput() throws IOException {
        ObjectOutput objectOutput = fastJsonSerialization.serialize(null, mock(OutputStream.class));
        assertThat(objectOutput, Matchers.<ObjectOutput>instanceOf(FastJsonObjectOutput.class));
    }

    @Test
    public void testObjectInput() throws IOException {
        ObjectInput objectInput = fastJsonSerialization.deserialize(null, mock(InputStream.class));
        assertThat(objectInput, Matchers.<ObjectInput>instanceOf(FastJsonObjectInput.class));
    }





    @Test
    public void testRomotingException() throws IOException, ClassNotFoundException {
        InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 20881);
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 20882);
        Throwable test = new Throwable("test");
        RemotingException remotingException = new RemotingException(localAddress, remoteAddress,
                test.getMessage(), test);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FastJsonObjectOutput fastJsonObjectOutput = new FastJsonObjectOutput(byteArrayOutputStream);
        fastJsonObjectOutput.writeObject(remotingException);

        byte[] bytes = byteArrayOutputStream.toByteArray();
        String s = new String(bytes);

        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        ParserConfig.getGlobalInstance().register(RemotingException.class.getName(), RemotingException.class);

        FastJsonObjectInput fastJsonObjectInput = new FastJsonObjectInput(new ByteArrayInputStream(bytes));
        Object object = fastJsonObjectInput.readObject();
        assertThat(object, Matchers.<Object>instanceOf(RemotingException.class));
        RemotingException readObject = (RemotingException)object;
        assertThat("local address empty", readObject.getLocalAddress() == null);
        assertThat("remote address empty", readObject.getRemoteAddress() == null);
        assertThat("no same message", readObject.getMessage().equals(remotingException.getMessage()));
        ByteArrayOutputStream printStreamResult = new ByteArrayOutputStream();
        readObject.getCause().printStackTrace(new PrintStream(printStreamResult));

        assertThat("no same stack", printStreamResult.size() > 0);
    }
}