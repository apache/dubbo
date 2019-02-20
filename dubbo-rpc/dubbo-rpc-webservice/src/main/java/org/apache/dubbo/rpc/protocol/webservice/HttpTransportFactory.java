package org.apache.dubbo.rpc.protocol.webservice;

import org.apache.cxf.Bus;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;

import java.util.List;

/**
 *
 */
public class HttpTransportFactory extends HTTPTransportFactory implements WSDLEndpointFactory {
    @Override
    public EndpointInfo createEndpointInfo(Bus bus, ServiceInfo serviceInfo, BindingInfo bindingInfo, List<?> list) {
        return super.createEndpointInfo(serviceInfo, bindingInfo, list);
    }

    @Override
    public void createPortExtensors(Bus bus, EndpointInfo endpointInfo, Service service) {
        super.createPortExtensors(endpointInfo, service);
    }
}
