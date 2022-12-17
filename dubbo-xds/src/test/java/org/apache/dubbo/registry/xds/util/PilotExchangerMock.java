package org.apache.dubbo.registry.xds.util;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.xds.util.protocol.impl.EdsProtocol;
import org.apache.dubbo.registry.xds.util.protocol.impl.LdsProtocol;
import org.apache.dubbo.registry.xds.util.protocol.impl.RdsProtocol;
import org.apache.dubbo.registry.xds.util.protocol.message.ListenerResult;
import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;

public class PilotExchangerMock extends PilotExchanger {

    public PilotExchangerMock(URL url) {
        super(url);
    }

    public XdsChannel getXdsChannel() {
        return xdsChannel;
    }

    public LdsProtocol getLdsProtocol() {
        return ldsProtocol;
    }

    public RdsProtocol getRdsProtocol() {
        return rdsProtocol;
    }

    public EdsProtocol getEdsProtocol() {
        return edsProtocol;
    }

    public ListenerResult getListenerResult() {
        return listenerResult;
    }

    public void setListenerResult(ListenerResult listenerResult) {
        this.listenerResult = listenerResult;
    }

    public RouteResult getRouteResult() {
        return routeResult;
    }

    public void setRouteResult(RouteResult routeResult) {
        this.routeResult = routeResult;
    }

    public PilotExchangerMock invokerSuperInitialize(URL url) {
        return new PilotExchangerMock(url);
    }
    public static PilotExchangerMock initialize(URL url) {
        return new PilotExchangerMock(url);
    }

}
