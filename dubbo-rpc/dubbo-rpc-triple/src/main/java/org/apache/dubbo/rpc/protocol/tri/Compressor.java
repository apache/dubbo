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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.dubbo.rpc.protocol.tri.Compressor.DEFAULT_COMPRESSOR;

/**
 * compress payload for grpc request， and decompress response payload
 * Configure it in files, pictures or other configurations that exist in the system properties
 * Configure {@link Constants#COMPRESSOR_KEY} in dubbo.properties、dubbo.yml or other configuration that exist in the system property
 */
@SPI(value = DEFAULT_COMPRESSOR, scope = ExtensionScope.FRAMEWORK)
public interface Compressor {

    Compressor NONE = new IdentityCompressor();

    String DEFAULT_COMPRESSOR = "identity";

    static Compressor getCompressor(FrameworkModel frameworkModel, String compressorStr) {
        if (null == compressorStr) {
            return null;
        }
        if (compressorStr.equals(DEFAULT_COMPRESSOR)) {
            return NONE;
        }
        return frameworkModel.getExtensionLoader(Compressor.class).getExtension(compressorStr);
    }

    static String getAcceptEncoding(FrameworkModel frameworkModel) {
        Set<Compressor> supportedEncodingSet = frameworkModel.getExtensionLoader(Compressor.class).getSupportedExtensionInstances();
        if (supportedEncodingSet.isEmpty()) {
            return null;
        }
        return supportedEncodingSet.stream().map(Compressor::getMessageEncoding).collect(Collectors.joining(","));
    }

    /**
     * message encoding of current compressor
     *
     * @return return message encoding
     */
    String getMessageEncoding();

    /**
     * compress payload
     *
     * @param payloadByteArr payload byte array
     * @return compressed payload byte array
     */
    byte[] compress(byte[] payloadByteArr);

    /**
     * decompress payload
     *
     * @param payloadByteArr payload byte array
     * @return decompressed payload byte array
     */
    byte[] decompress(byte[] payloadByteArr);
}
