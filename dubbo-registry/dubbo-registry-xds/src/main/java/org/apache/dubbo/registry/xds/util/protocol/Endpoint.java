package org.apache.dubbo.registry.xds.util.protocol;

import java.util.Objects;

public class Endpoint {
    private String address;
    private int portValue;
    private boolean healthy;
    private int weight;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPortValue() {
        return portValue;
    }

    public void setPortValue(int portValue) {
        this.portValue = portValue;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "address='" + address + '\'' +
                ", portValue='" + portValue + '\'' +
                ", healthy=" + healthy +
                ", weight=" + weight +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Endpoint endpoint = (Endpoint) o;
        return healthy == endpoint.healthy &&
                weight == endpoint.weight &&
                Objects.equals(address, endpoint.address) &&
                Objects.equals(portValue, endpoint.portValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, portValue, healthy, weight);
    }
}
