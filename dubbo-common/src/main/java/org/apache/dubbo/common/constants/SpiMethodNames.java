package org.apache.dubbo.common.constants;

public enum SpiMethodNames {

    //dubbo-config-api
    publishServiceDefinition(PackageName.CONFIG_API,"org.apache.dubbo.registry.spi.PublishServiceDefinition"),

    //dubbo-monitor
    isSupportMonitor(PackageName.MONITOR,"org.apache.dubbo.monitor.spi.IsSupportMonitor"),

    loadMonitor(PackageName.MONITOR,"org.apache.dubbo.monitor.spi.LoadMonitor"),


    //dubbo-registry-api
    loadRegistry(PackageName.REGISTRY_API,"org.apache.dubbo.registry.spi.LoadRegistry"),

    toRsEvent(PackageName.REGISTRY_API,"org.apache.dubbo.metrics.registry.spi.ToRsEvent"),


    //dubbo-rpc-api
    destroyProtocols(PackageName.PRC_API,"org.apache.dubbo.rpc.spi.DestroyProtocols"),


    //dubbo-metrics-api
    publishMetricsEvent(PackageName.METRICS_API,"org.apache.dubbo.metrics.spi.PublishMetricsEvent"),

    postMetricsEvent(PackageName.METRICS_API,"org.apache.dubbo.metrics.spi.PostMetricsEvent"),


    //dubbo-cluster
    createClusterInvoker(PackageName.CLUSTER,"org.apache.dubbo.rpc.cluster.spi.CreateClusterInvoker"),

    getConfiguratorUrl(PackageName.CLUSTER,"org.apache.dubbo.rpc.cluster.spi.GetConfiguratorUrl"),

    mergeUrl(PackageName.CLUSTER,"org.apache.dubbo.rpc.cluster.spi.MergeUrl"),

    checkClusterExtension(PackageName.CLUSTER,"org.apache.dubbo.rpc.cluster.spi.CheckClusterExtension"),

    checkClusterFilterExtension(PackageName.CLUSTER,"org.apache.dubbo.rpc.cluster.spi.CheckClusterFilterExtension"),


    //dubbo-rpc-injvm
    isJvmRefer(PackageName.RPC_INJVM,"org.apache.dubbo.rpc.protocol.injvm.spi.IsJvmRefer");

    SpiMethodNames(PackageName packageName, String methodName) {
        this.packageName = packageName;
        this.methodName = methodName;
    }

    private final PackageName packageName;

    private final String methodName;

    public PackageName getPackageName() {
        return packageName;
    }

}
