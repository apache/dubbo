package org.apache.dubbo.rpc.protocol.jsonrpc;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class RpcContextFilterTest {

    @Test
    public void test_ParseFromStr() {

        String v = "k1=v1,v2,k3=v3";
        Map<String, String> m = RpcContextFilter.parse(v);
        assertEquals("v1", m.get("k1"));
        assertEquals("v3", m.get("k3"));
    }

    @Test
    public void test_ParseFromMap() {

        Map<String, String> attachment = new HashMap<>();
        attachment.put("k1", "v1");
        attachment.put("k2", "v2");
        attachment.put("k3", "v3");

        String v = RpcContextFilter.parse(attachment);

        assertEquals("k1=v1,k2=v2,k3=v3", v);
    }
}
