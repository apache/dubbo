package org.apache.dubbo.metadata.metadata.builder;

import org.apache.dubbo.metadata.metadata.ServiceDescriptor;
import org.junit.Assert;
import org.junit.Test;

/**
 *  2018/9/29
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
