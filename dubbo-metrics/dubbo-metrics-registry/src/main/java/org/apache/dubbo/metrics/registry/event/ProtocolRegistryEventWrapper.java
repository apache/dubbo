package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;


@Activate(order = 50)
public class ProtocolRegistryEventWrapper implements Protocol {

    private final Protocol protocol;

    private final ApplicationModel applicationModel;

    public ProtocolRegistryEventWrapper(Protocol protocol,ApplicationModel applicationModel) {
        this.protocol = protocol;
        this.applicationModel = applicationModel;
    }

    @Override
    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        //TODO
        URL url = invoker.getUrl();
        String serviceKey = null;

        return MetricsEventBus.post(
            RegistryEvent.toRsEvent(applicationModel, serviceKey, 1),
            () -> this.protocol.export(invoker)
        );
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        return protocol.refer(type, url);
    }

    @Override
    public void destroy() {
        protocol.destroy();
    }
}
