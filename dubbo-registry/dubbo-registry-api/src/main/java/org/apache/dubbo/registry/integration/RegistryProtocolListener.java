package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invoker;

/**
 * RegistryProtocol listener is introduced to provide a chance to user to customize or change export and refer behavior
 * of RegistryProtocol. For example: re-export or re-refer on the fly when certain condition meets.
 */
@SPI
public interface RegistryProtocolListener {
    /**
     * Notify RegistryProtocol's listeners when a service is registered
     *
     * @param registryProtocol RegistryProtocol instance
     * @param invoker          original invoker used to export
     * @param url              provider's URL used to register on registry center
     * @see RegistryProtocol#export(org.apache.dubbo.rpc.Invoker)
     */
    void onExport(RegistryProtocol registryProtocol, Invoker<?> invoker, URL url);

    /**
     * Notify RegistryProtocol's listeners when a service is subscribed
     *
     * @param registryProtocol RegistryProtocol instance
     * @param url              consumer's URL used to subscribe on registry center
     */
    void onRefer(RegistryProtocol registryProtocol, URL url);
}
