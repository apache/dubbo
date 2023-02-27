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

package org.apache.dubbo.rpc.protocol.tri.compressor;

import org.apache.dubbo.rpc.RpcException;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;


/**
 * bzip2 compressor, faster compression efficiency
 *
 * @link https://commons.apache.org/proper/commons-compress/
 */
public class Bzip2 implements Compressor, DeCompressor {

    public static final String BZIP2 = "bzip2";

    @Override
    public String getMessageEncoding() {
        return BZIP2;
    }

    @Override
    public byte[] compress(byte[] payloadByteArr) throws RpcException {
        if (null == payloadByteArr || 0 == payloadByteArr.length) {
            return new byte[0];
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BZip2CompressorOutputStream cos;
        try {
            cos = new BZip2CompressorOutputStream(out);
            cos.write(payloadByteArr);
            cos.close();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return out.toByteArray();
    }

    @Override
    public byte[] decompress(byte[] payloadByteArr) {
        if (null == payloadByteArr || 0 == payloadByteArr.length) {
            return new byte[0];
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(payloadByteArr);
        try {
            BZip2CompressorInputStream unZip = new BZip2CompressorInputStream(in);
            byte[] buffer = new byte[2048];
            int n;
            while ((n = unZip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }
}
