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

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * bzip2 compressor, faster compression efficiency
 *
 * @link https://commons.apache.org/proper/commons-compress/
 */
public class Bzip2 implements Compressor, DeCompressor {

    public static final String BZIP2 = "bzip2";

    private final int maxMessageSize;

    @Override
    public String getMessageEncoding() {
        return BZIP2;
    }

    public Bzip2() {
        Configuration conf = ConfigurationUtils.getEnvConfiguration(ApplicationModel.defaultModel());
        this.maxMessageSize = conf.getInteger(Constants.H2_SETTINGS_MAX_MESSAGE_SIZE, 50 * 1024 * 1024);
    }

    @Override
    public byte[] compress(byte[] payloadByteArr) throws RpcException {
        if (null == payloadByteArr || 0 == payloadByteArr.length) {
            return new byte[0];
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (BZip2CompressorOutputStream cos = new BZip2CompressorOutputStream(out)) {
                cos.write(payloadByteArr);
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public OutputStream decorate(OutputStream outputStream) {
        try {
            return new BZip2CompressorOutputStream(outputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public byte[] decompress(byte[] payloadByteArr) {
        if (null == payloadByteArr || 0 == payloadByteArr.length) {
            return new byte[0];
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                ByteArrayInputStream in = new ByteArrayInputStream(payloadByteArr);
                BZip2CompressorInputStream unZip = new BZip2CompressorInputStream(in)) {
            int totalBytesRead = 0;
            byte[] buffer = new byte[2048];
            int n;
            while ((n = unZip.read(buffer)) >= 0) {
                totalBytesRead += n;
                if (totalBytesRead > maxMessageSize) {
                    throw new RpcException("Decompressed message size " + totalBytesRead
                            + " exceeds the maximum configured message size " + maxMessageSize);
                }
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
