package org.apache.dubbo.rpc.support;

/**
 * default mock service for DemoServiceA
 */
public class DemoServiceAMock implements DemoServiceA{
    public static final String MOCK_VALUE = "mockA";
    @Override
    public String methodA() {
        return MOCK_VALUE;
    }
}
