package org.apache.dubbo.rpc.protocol.rsocket;

import org.apache.dubbo.rpc.Invocation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author sixie.xyn on 2019/1/3.
 */
public class MetadataCodec {

    public static Map<String, Object> decodeMetadata(byte[] bytes) throws IOException {
        return JSON.parseObject(new String(bytes, StandardCharsets.UTF_8), Map.class);
    }

    public static byte[] encodeMetadata(Map<String, Object> metadata) throws IOException {
        String jsonStr = JSON.toJSONString(metadata);
        return jsonStr.getBytes(StandardCharsets.UTF_8);
    }

}
