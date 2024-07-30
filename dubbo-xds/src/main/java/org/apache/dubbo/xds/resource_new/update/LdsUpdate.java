package org.apache.dubbo.xds.resource_new.update;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.xds.resource_new.listener.HttpConnectionManager;
import org.apache.dubbo.xds.resource_new.listener.Listener;

import java.util.Objects;

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
        Assert.notNull(httpConnectionManager, "httpConnectionManager must not be null");
        return new LdsUpdate(httpConnectionManager, null);
    }

    public static LdsUpdate forTcpListener(Listener listener) {
        Assert.notNull(listener, "listener must not be null");
        return new LdsUpdate(null, listener);
    }
}
