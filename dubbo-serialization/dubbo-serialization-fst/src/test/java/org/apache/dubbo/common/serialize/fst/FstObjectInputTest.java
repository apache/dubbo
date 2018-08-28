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

import org.apache.dubbo.common.serialize.fst.model.AnimalEnum;
import org.apache.dubbo.common.serialize.fst.model.FullAddress;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class FstObjectInputTest {
    private FstObjectInput fstObjectInput;

    @Test(expected = IOException.class)
    public void testWrongClassInput() throws IOException, ClassNotFoundException {
        this.fstObjectInput = new FstObjectInput(new ByteArrayInputStream("{animal: 'cat'}".getBytes()));

        fstObjectInput.readObject(FullAddress.class);
    }

    @Test
    public void testEmptyByteArrayForEmptyInput() throws IOException {
        this.fstObjectInput = new FstObjectInput(new ByteArrayInputStream("".getBytes()));

        byte[] bytes = fstObjectInput.readBytes();
        assertThat(bytes.length, is(0));
    }


}