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

package com.alibaba.com.caucho.hessian.io;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.EnumSet;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class Hessian2EnumSetTest {

    @Test
    public void singleton() throws Exception {
        EnumSet h = EnumSet.of(Type.High);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bout);

        out.writeObject(h);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        Hessian2Input input = new Hessian2Input(bin);
        EnumSet set = (EnumSet) input.readObject();

        assertTrue(Arrays.asList(set.toArray()).contains(Type.High));
        assertFalse(Arrays.asList(set.toArray()).contains(Type.Lower));
    }

    @Test
    public void set() throws Exception {
        EnumSet<Type> types = EnumSet.of(Type.High, Type.Lower);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bout);

        out.writeObject(types);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        Hessian2Input input = new Hessian2Input(bin);

        EnumSet set = (EnumSet) input.readObject();
        assertTrue(set.contains(Type.High));
        assertFalse(set.contains(Type.Normal));
    }

    @Test
    public void none() throws Exception {
        EnumSet<Type> types = EnumSet.noneOf(Type.class);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bout);

        out.writeObject(types);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        Hessian2Input input = new Hessian2Input(bin);

        EnumSet set = (EnumSet) input.readObject();
        TestCase.assertEquals(set, EnumSet.noneOf(Type.class));
    }
}
