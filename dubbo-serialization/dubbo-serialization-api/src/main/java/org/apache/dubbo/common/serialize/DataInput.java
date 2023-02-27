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
 * Basic data type input interface.
 */
public interface DataInput {

    /**
     * Read boolean.
     *
     * @return boolean.
     * @throws IOException
     */
    boolean readBool() throws IOException;

    /**
     * Read byte.
     *
     * @return byte value.
     * @throws IOException
     */
    byte readByte() throws IOException;

    /**
     * Read short integer.
     *
     * @return short.
     * @throws IOException
     */
    short readShort() throws IOException;

    /**
     * Read integer.
     *
     * @return integer.
     * @throws IOException
     */
    int readInt() throws IOException;

    /**
     * Read long.
     *
     * @return long.
     * @throws IOException
     */
    long readLong() throws IOException;

    /**
     * Read float.
     *
     * @return float.
     * @throws IOException
     */
    float readFloat() throws IOException;

    /**
     * Read double.
     *
     * @return double.
     * @throws IOException
     */
    double readDouble() throws IOException;

    /**
     * Read UTF-8 string.
     *
     * @return string.
     * @throws IOException
     */
    String readUTF() throws IOException;

    /**
     * Read byte array.
     *
     * @return byte array.
     * @throws IOException
     */
    byte[] readBytes() throws IOException;
}
