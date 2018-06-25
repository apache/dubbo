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
import org.mockito.Mockito;

import static org.apache.dubbo.registry.support.ProviderConsumerRegTable.getProviderInvoker;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OfflineTest {
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
        for (ProviderInvokerWrapper wrapper : getProviderInvoker("org.apache.dubbo.BarService")) {
            wrapper.setReg(true);
        }

        Registry registry = mock(Registry.class);
        TestRegistryFactory.registry = registry;

        Offline offline = new Offline();
        String output = offline.execute(mock(CommandContext.class), new String[]{"org.apache.dubbo.BarService"});
        assertThat(output, containsString("OK"));
        Mockito.verify(registry).unregister(providerUrl);
        for (ProviderInvokerWrapper wrapper : getProviderInvoker("org.apache.dubbo.BarService")) {
            assertThat(wrapper.isReg(), is(false));
        }

        output = offline.execute(mock(CommandContext.class), new String[]{"org.apache.dubbo.FooService"});
        assertThat(output, containsString("service not found"));
    }
}
