package org.apache.dubbo.xds.resource.grpc.resource.update;

import org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.HttpConnectionManager;
import org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.Listener;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class LdsUpdate implements ResourceUpdate {

    private HttpConnectionManager httpConnectionManager;
    private Listener listener;

    public LdsUpdate(
            HttpConnectionManager httpConnectionManager, Listener listener) {
        this.httpConnectionManager = httpConnectionManager;
        this.listener = listener;
    }

    public HttpConnectionManager getHttpConnectionManager() {
        return httpConnectionManager;
    }

    public void setHttpConnectionManager(HttpConnectionManager httpConnectionManager) {
        this.httpConnectionManager = httpConnectionManager;
    }

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public String toString() {
        return "XdsListenerResourceLdsUpdate{" + "httpConnectionManager=" + httpConnectionManager + ", " + "listener="
                + listener + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof LdsUpdate)) {return false;}
        LdsUpdate that = (LdsUpdate) o;
        return Objects.equals(httpConnectionManager, that.httpConnectionManager)
                && Objects.equals(listener, that.listener);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpConnectionManager, listener);
    }

    public static LdsUpdate forApiListener(HttpConnectionManager httpConnectionManager) {
        checkNotNull(httpConnectionManager, "httpConnectionManager");
        return new LdsUpdate(httpConnectionManager, null);
    }

    public static LdsUpdate forTcpListener(Listener listener) {
        checkNotNull(listener, "listener");
        return new LdsUpdate(null, listener);
    }
}
