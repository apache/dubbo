package org.apache.dubbo.registry.xds.util.bootstrap;

import io.grpc.ChannelCredentials;

final class ServerInfoImpl extends Bootstrapper.ServerInfo {

    private final String target;

    private final ChannelCredentials channelCredentials;

    private final boolean useProtocolV3;

    private final boolean ignoreResourceDeletion;

    ServerInfoImpl(String target, ChannelCredentials channelCredentials, boolean useProtocolV3, boolean ignoreResourceDeletion) {
        this.target = target;
        this.channelCredentials = channelCredentials;
        this.useProtocolV3 = useProtocolV3;
        this.ignoreResourceDeletion = ignoreResourceDeletion;
    }

    @Override
    String target() {
        return target;
    }

    @Override
    ChannelCredentials channelCredentials() {
        return channelCredentials;
    }

    @Override
    boolean useProtocolV3() {
        return useProtocolV3;
    }

    @Override
    boolean ignoreResourceDeletion() {
        return ignoreResourceDeletion;
    }

    @Override
    public String toString() {
        return "ServerInfo{"
            + "target=" + target + ", "
            + "channelCredentials=" + channelCredentials + ", "
            + "useProtocolV3=" + useProtocolV3 + ", "
            + "ignoreResourceDeletion=" + ignoreResourceDeletion
            + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Bootstrapper.ServerInfo) {
            Bootstrapper.ServerInfo that = (Bootstrapper.ServerInfo) o;
            return this.target.equals(that.target())
                && this.channelCredentials.equals(that.channelCredentials())
                && this.useProtocolV3 == that.useProtocolV3()
                && this.ignoreResourceDeletion == that.ignoreResourceDeletion();
        }
        return false;
    }

}
