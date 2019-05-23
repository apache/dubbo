package org.apache.dubbo.rpc.protocol.dubbo;

import java.util.Objects;

public final class DubboExporterKey {
    private final int port;
    private final String path;
    private final String version;
    private final String group;

    public DubboExporterKey(int port, String path, String version, String group) {
        this.port = port;
        this.path = path;
        this.version = version;
        this.group = group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DubboExporterKey that = (DubboExporterKey) o;
        return port == that.port &&
                path.equals(that.path) &&
                Objects.equals(version, that.version) &&
                Objects.equals(group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, path, version, group);
    }

    @Override
    public String toString() {
        return "DubboExporterKey{" +
                "port=" + port +
                ", path='" + path + '\'' +
                ", version='" + version + '\'' +
                ", group='" + group + '\'' +
                '}';
    }
}