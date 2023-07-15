package org.apache.dubbo.common.constants;

public enum PackageName {

    DEFAULT("default"),

    CONFIG_API("dubbo-config-api"),

    REGISTRY_API("dubbo-registry-api"),

    MONITOR("dubbo-monitor"),

    PRC_API("dubbo-rpc-api"),

    RPC_INJVM("dubbo-rpc-injvm"),

    METRICS_REGISTRY("dubbo-metrics-registry"),

    METRICS_API("dubbo-metrics-api"),

    CLUSTER("dubbo-cluster");

    final String name;

    PackageName(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
