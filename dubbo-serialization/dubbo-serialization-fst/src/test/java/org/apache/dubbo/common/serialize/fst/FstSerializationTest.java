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
package org.apache.dubbo.common.serialize.fst;

import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

public class FstSerializationTest {
    private FstSerialization fstSerialization;

    @Before
    public void setUp() {
        this.fstSerialization = new FstSerialization();
    }

    @Test
    public void testContentTypeId() {
        assertThat(fstSerialization.getContentTypeId(), is((byte) 9));
    }

    @Test
    public void testContentType() {
        assertThat(fstSerialization.getContentType(), is("x-application/fst"));
    }

    @Test
    public void testSerialize() throws IOException {
        ObjectOutput objectOutput = fstSerialization.serialize(null, mock(OutputStream.class));
        assertThat(objectOutput, Matchers.<ObjectOutput>instanceOf(FstObjectOutput.class));
    }

    @Test
    public void testDeserialize() throws IOException {
        ObjectInput objectInput = fstSerialization.deserialize(null, mock(InputStream.class));
        assertThat(objectInput, Matchers.<ObjectInput>instanceOf(FstObjectInput.class));
    }
}