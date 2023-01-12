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
package org.apache.dubbo.common.serialize.hessian2;

import org.apache.dubbo.common.serialize.Cleanable;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.rpc.model.FrameworkModel;

import com.alibaba.com.caucho.hessian.io.Hessian2Output;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Hessian2 object output implementation
 */
public class Hessian2ObjectOutput implements ObjectOutput, Cleanable {

    private final Hessian2Output mH2o;

    @Deprecated
    public Hessian2ObjectOutput(OutputStream os) {
        mH2o = new Hessian2Output(os);
        Hessian2FactoryManager hessian2FactoryManager = FrameworkModel.defaultModel().getBeanFactory().getOrRegisterBean(Hessian2FactoryManager.class);
        mH2o.setSerializerFactory(hessian2FactoryManager.getSerializerFactory(Thread.currentThread().getContextClassLoader()));
    }

    public Hessian2ObjectOutput(OutputStream os, Hessian2FactoryManager hessian2FactoryManager) {
        mH2o = new Hessian2Output(os);
        mH2o.setSerializerFactory(hessian2FactoryManager.getSerializerFactory(Thread.currentThread().getContextClassLoader()));
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        mH2o.writeBoolean(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        mH2o.writeInt(v);
    }

    @Override
    public void writeShort(short v) throws IOException {
        mH2o.writeInt(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        mH2o.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        mH2o.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        mH2o.writeDouble(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        mH2o.writeDouble(v);
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        mH2o.writeBytes(b);
    }

    @Override
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        mH2o.writeBytes(b, off, len);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        mH2o.writeString(v);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        mH2o.writeObject(obj);
    }

    @Override
    public void flushBuffer() throws IOException {
        mH2o.flushBuffer();
    }

    public OutputStream getOutputStream() throws IOException {
        return mH2o.getBytesOutputStream();
    }

    @Override
    public void cleanup() {
        if(mH2o != null) {
            mH2o.reset();
        }
    }
}
