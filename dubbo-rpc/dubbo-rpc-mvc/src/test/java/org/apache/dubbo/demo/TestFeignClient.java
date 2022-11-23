package org.apache.dubbo.demo;

import org.apache.dubbo.rpc.protocol.mvc.feign.FeignClientBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFeignClient {

    @Test
    public void testFeignCall() {
        SpringmvcDemoService feignClient = FeignClientBuilder.createFeignClient(SpringmvcDemoService.class, "http://localhost:8083");

        Integer hello = feignClient.hello(1, 2);

        assertEquals(3, hello);
    }
}
