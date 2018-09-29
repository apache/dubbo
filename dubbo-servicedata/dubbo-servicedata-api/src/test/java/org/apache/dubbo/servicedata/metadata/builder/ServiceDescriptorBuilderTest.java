package org.apache.dubbo.servicedata.metadata.builder;

import org.apache.dubbo.servicedata.metadata.ServiceDescriptor;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author cvictory ON 2018/9/29
 */
public class ServiceDescriptorBuilderTest {

    @Test
    public void testGetCodeSource(){
        String codeSource = ServiceDescriptorBuilder.getCodeSource(ServiceDescriptorBuilder.class);
        Assert.assertNotNull(codeSource);
    }

    @Test
    public void testBuild(){
        ServiceDescriptor serviceDescriptor = ServiceDescriptorBuilder.build(TestService.class);
        Assert.assertNotNull(serviceDescriptor);
        Assert.assertTrue(serviceDescriptor.getMethodDescriptors().size() == 17);
        Assert.assertTrue(serviceDescriptor.getTypes().size() == 4);
    }
}
