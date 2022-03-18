package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class StubSuppliers {
    private static final Map<String, Function<Invoker<?>, Object>> STUB_SUPPLIERS = new ConcurrentHashMap<>();
    private static final Map<String, ServiceDescriptor> SERVICE_DESCRIPTOR_MAP = new ConcurrentHashMap<>();

    public static void addDescriptor(String interfaceName, ServiceDescriptor serviceDescriptor) {
        SERVICE_DESCRIPTOR_MAP.put(interfaceName, serviceDescriptor);
    }

    public static void addSupplier(String interfaceName, Function<Invoker<?>, Object> supplier) {
        STUB_SUPPLIERS.put(interfaceName, supplier);
    }

    public static <T> T createStub(String interfaceName, Invoker<T> invoker) {
        ReflectUtils.forName(interfaceName.substring(0, interfaceName.lastIndexOf('$')));
        if (!STUB_SUPPLIERS.containsKey(interfaceName)) {
            throw new IllegalStateException("Can not find any stub supplier for " + interfaceName);
        }
        return (T) STUB_SUPPLIERS.get(interfaceName).apply(invoker);
    }

    public static ServiceDescriptor getServiceDescriptor(String interfaceName) {
        ReflectUtils.forName(interfaceName.substring(0, interfaceName.lastIndexOf('$')));
        if (!SERVICE_DESCRIPTOR_MAP.containsKey(interfaceName)) {
            throw new IllegalStateException("Can not find any stub supplier for " + interfaceName);
        }
        return SERVICE_DESCRIPTOR_MAP.get(interfaceName);
    }
}
