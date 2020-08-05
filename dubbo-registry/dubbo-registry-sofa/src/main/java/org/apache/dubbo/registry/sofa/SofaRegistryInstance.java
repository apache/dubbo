package org.apache.dubbo.registry.sofa;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: hongwei.quhw
 * @date: 2020-08-06 14:26
 */
public class SofaRegistryInstance {
    private String id;

    private String host;

    private int port;

    private String name;

    private Map<String, String> metadata = new HashMap<>();

    private SofaRegistryInstance() {
    }

    public SofaRegistryInstance(String id, String host, int port, String name, Map<String, String> metadata) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.name = name;
        this.metadata = metadata;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "SofaRegistryInstance{" + "id='" + this.id + '\''+ "host='" + this.host + '\'' + "port='" + this.port + '\''+ ", name='" + this.name
                + '\'' + ", metadata=" + this.metadata + '}';
    }
}
