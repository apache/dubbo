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
package org.apache.dubbo.metadata.annotation.processing.model;

/**
 * Primitive Type model
 *
 * @since 2.7.6
 */
public class PrimitiveTypeModel {

    private boolean z;

    private byte b;

    private char c;

    private short s;

    private int i;

    private long l;

    private float f;

    private double d;

    public boolean isZ() {
        return z;
    }

    public byte getB() {
        return b;
    }

    public char getC() {
        return c;
    }

    public short getS() {
        return s;
    }

    public int getI() {
        return i;
    }

    public long getL() {
        return l;
    }

    public float getF() {
        return f;
    }

    public double getD() {
        return d;
    }
}
