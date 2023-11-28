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
package org.apache.dubbo.common.serialize;

import java.io.IOException;

/**
 * Basic default json type input interface.
 */
public interface DefaultJsonDataInput extends ObjectInput {

    @Override
    default boolean readBool() throws IOException {
        return readObject(boolean.class);
    }

    @Override
    default byte readByte() throws IOException {
        return readObject(byte.class);
    }

    @Override
    default short readShort() throws IOException {
        return readObject(short.class);
    }

    @Override
    default int readInt() throws IOException {
        return readObject(int.class);
    }

    @Override
    default long readLong() throws IOException {
        return readObject(long.class);
    }

    @Override
    default float readFloat() throws IOException {
        return readObject(float.class);
    }

    @Override
    default double readDouble() throws IOException {
        return readObject(double.class);
    }

    @Override
    default String readUTF() throws IOException {
        return readObject(String.class);
    }

    <T> T readObject(Class<T> cls) throws IOException;

    @Override
    default Object readObject() throws IOException {
        return readObject(Object.class);
    }




}
