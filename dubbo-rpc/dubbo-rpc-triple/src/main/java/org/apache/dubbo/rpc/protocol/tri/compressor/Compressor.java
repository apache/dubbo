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

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.model.FrameworkModel;

/**
 * compress payload for grpc request， and decompress response payload Configure it in files,
 * pictures or other configurations that exist in the system properties Configure {@link
 * Constants#COMPRESSOR_KEY} in dubbo.properties、dubbo.yml or other configuration that exist in the
 * system property
 */
@SPI(scope = ExtensionScope.FRAMEWORK)
public interface Compressor extends MessageEncoding {

    Compressor NONE = Identity.IDENTITY;

    static Compressor getCompressor(FrameworkModel frameworkModel, String compressorStr) {
        if (null == compressorStr) {
            return null;
        }
        if (compressorStr.equals(Identity.MESSAGE_ENCODING)) {
            return NONE;
        }
        return frameworkModel.getExtensionLoader(Compressor.class).getExtension(compressorStr);
    }

    /**
     * compress payload
     *
     * @param payloadByteArr payload byte array
     * @return compressed payload byte array
     */
    byte[] compress(byte[] payloadByteArr);

}
