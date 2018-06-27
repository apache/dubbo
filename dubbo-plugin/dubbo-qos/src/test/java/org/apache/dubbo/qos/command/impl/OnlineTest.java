package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.model.ApplicationModel;
import org.apache.dubbo.config.model.ProviderModel;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.ProviderConsumerRegTable;
import org.apache.dubbo.registry.support.ProviderInvokerWrapper;
import org.apache.dubbo.rpc.Invoker;
import org.junit.Test;

import static org.apache.dubbo.registry.support.ProviderConsumerRegTable.getProviderInvoker;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OnlineTest {
    @Test
    public void testExecute() throws Exception {
        ProviderModel providerModel = mock(ProviderModel.class);
        when(providerModel.getServiceName()).thenReturn("org.apache.dubbo.BarService");
        ApplicationModel.initProviderModel("org.apache.dubbo.BarService", providerModel);

        Invoker providerInvoker = mock(Invoker.class);
        URL registryUrl = mock(URL.class);
        when(registryUrl.toFullString()).thenReturn("test://localhost:8080");
        URL providerUrl = mock(URL.class);
        when(providerUrl.getServiceKey()).thenReturn("org.apache.dubbo.BarService");
        when(providerUrl.toFullString()).thenReturn("dubbo://localhost:8888/org.apache.dubbo.BarService");
        when(providerInvoker.getUrl()).thenReturn(providerUrl);
        ProviderConsumerRegTable.registerProvider(providerInvoker, registryUrl, providerUrl);

        Registry registry = mock(Registry.class);
        TestRegistryFactory.registry = registry;

        Online online = new Online();
        String output = online.execute(mock(CommandContext.class), new String[]{"org.apache.dubbo.BarService"});
        assertThat(output, equalTo("OK"));
        for (ProviderInvokerWrapper wrapper : getProviderInvoker("org.apache.dubbo.BarService")) {
            assertTrue(wrapper.isReg());
        }
    }
}
