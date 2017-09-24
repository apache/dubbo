package com.alibaba.dubbo.registry.common.util;

import com.alibaba.dubbo.common.io.Bytes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Coder {

    private Coder() {
    }

    public static String encodeHex(byte[] bytes) {
        StringBuffer buffer = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            if (((int) bytes[i] & 0xff) < 0x10)
                buffer.append("0");
            buffer.append(Long.toString((int) bytes[i] & 0xff, 16));
        }
        return buffer.toString();
    }

    public static String encodeMd5(String source) {
        return encodeMd5(source.getBytes());
    }

    public static String encodeMd5(byte[] source) {
        try {
            return encodeHex(MessageDigest.getInstance("MD5").digest(source));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static String encodeBase64(String source) {
        return Bytes.bytes2base64(source.getBytes());
    }

    public static String decodeBase64(String source) {
        return new String(Bytes.base642bytes(source));
    }

}
