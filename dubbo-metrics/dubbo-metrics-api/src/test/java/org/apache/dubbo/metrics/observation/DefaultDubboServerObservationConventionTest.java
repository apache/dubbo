package org.apache.dubbo.metrics.observation;

import io.micrometer.common.KeyValues;
import org.apache.dubbo.metrics.observation.utils.ObservationConventionUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
public class DefaultDubboServerObservationConventionTest {

    static DubboServerObservationConvention dubboServerObservationConvention = DefaultDubboServerObservationConvention.getInstance();

    @Test
    void testGetName() {
        Assertions.assertEquals("rpc.server.duration", dubboServerObservationConvention.getName());
    }

    @Test
    void testGetLowCardinalityKeyValues() throws NoSuchFieldException, IllegalAccessException {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("testMethod");
        invocation.setAttachment("interface", "com.example.TestService");
        invocation.setTargetServiceUniqueName("targetServiceName1");

        Invoker<?> invoker = ObservationConventionUtils.getMockInvokerWithUrl();
        invocation.setInvoker(invoker);

        DubboServerContext context = new DubboServerContext(invoker, invocation);

        KeyValues keyValues = dubboServerObservationConvention.getLowCardinalityKeyValues(context);

        Assertions.assertEquals("testMethod", ObservationConventionUtils.getValueForKey(keyValues, "rpc.method"));
        Assertions.assertEquals("targetServiceName1", ObservationConventionUtils.getValueForKey(keyValues, "rpc.service"));
        Assertions.assertEquals("apache_dubbo", ObservationConventionUtils.getValueForKey(keyValues, "rpc.system"));
    }

    @Test
    void testGetContextualName() {
        RpcInvocation invocation = new RpcInvocation();
        Invoker<?> invoker = ObservationConventionUtils.getMockInvokerWithUrl();
        invocation.setMethodName("testMethod");
        invocation.setServiceName("com.example.TestService");

        DubboClientContext context = new DubboClientContext(invoker, invocation);

        DefaultDubboClientObservationConvention convention = new DefaultDubboClientObservationConvention();

        String contextualName = convention.getContextualName(context);
        Assertions.assertEquals("com.example.TestService/testMethod", contextualName);
    }
}
