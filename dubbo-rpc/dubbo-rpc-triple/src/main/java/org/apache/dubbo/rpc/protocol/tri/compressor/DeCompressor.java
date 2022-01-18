package org.apache.dubbo.rpc.protocol.tri.compressor;


import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Set;
import java.util.stream.Collectors;

@SPI(scope = ExtensionScope.FRAMEWORK)
public interface DeCompressor extends MessageEncoding {

    DeCompressor NONE = Identity.IDENTITY;

    /**
     * decompress payload
     *
     * @param payloadByteArr payload byte array
     * @return decompressed payload byte array
     */
    byte[] decompress(byte[] payloadByteArr);

    static DeCompressor getCompressor(FrameworkModel frameworkModel, String compressorStr) {
        if (null == compressorStr) {
            return null;
        }
        if (compressorStr.equals(Identity.MESSAGE_ENCODING)) {
            return NONE;
        }
        return frameworkModel.getExtensionLoader(DeCompressor.class).getExtension(compressorStr);
    }

    static String getAcceptEncoding(FrameworkModel frameworkModel) {
        Set<DeCompressor> supportedEncodingSet = frameworkModel.getExtensionLoader(DeCompressor.class).getSupportedExtensionInstances();
        if (supportedEncodingSet.isEmpty()) {
            return null;
        }
        return supportedEncodingSet.stream().map(DeCompressor::getMessageEncoding).collect(Collectors.joining(","));
    }


}
