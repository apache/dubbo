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
package org.apache.dubbo.common.serialize.java;


import org.apache.dubbo.common.serialize.nativejava.NativeJavaObjectOutput;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Java Object output.
 */
public class JavaObjectOutput extends NativeJavaObjectOutput {
    public JavaObjectOutput(OutputStream os) throws IOException {
        super(new ObjectOutputStream(os));
    }

    public JavaObjectOutput(OutputStream os, boolean compact) throws IOException {
        super(compact ? new CompactedObjectOutputStream(os) : new ObjectOutputStream(os));
    }

    @Override
    public void writeUTF(String v) throws IOException {
        if (v == null) {
            getObjectOutputStream().writeInt(-1);
        } else {
            getObjectOutputStream().writeInt(v.length());
            getObjectOutputStream().writeUTF(v);
        }
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        if (obj == null) {
            getObjectOutputStream().writeByte(0);
        } else {
            getObjectOutputStream().writeByte(1);
            getObjectOutputStream().writeObject(obj);
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        getObjectOutputStream().flush();
    }
}
