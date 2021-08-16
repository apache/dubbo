package org.apache.dubbo.rpc.support;

/**
 * default mock service for DemoServiceA
 */
public class DemoServiceBMock implements DemoServiceB {
    public static final String MOCK_VALUE = "mockB";

    @Override
    public String methodB() {
        return MOCK_VALUE;
    }
}
