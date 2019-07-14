package org.apache.dubbo.rpc.protocol.rsocket;

import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
public class MetadataCodecTest {

    @Test
    public void decodeMetadata() throws IOException {
        String str = "{\"小张\": \"dubbo\"}";

        Map<String, Object> map = MetadataCodec.decodeMetadata(str.getBytes(StandardCharsets.UTF_8));
        assertThat(map, IsMapContaining.hasEntry("小张", "dubbo"));
    }

    @Test
    public void encodeMetadata() throws IOException {
        String str = "{\"小张\":\"dubbo\"}";
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("小张", "dubbo");
        assertEquals(new String(MetadataCodec.encodeMetadata(testMap)), str);

    }
}