package com.alibaba.dubbo.config.model;

import com.alibaba.dubbo.config.ReferenceConfig;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author qinliujie
 * @date 2017/11/22
 */
public class ConsumerModel {
    private ReferenceConfig metadata;
    private Object proxyObject;
    private String serviceName;

    private final Map<Method, ConsumerMethodModel> methodModels = new IdentityHashMap<Method, ConsumerMethodModel>();

    public ConsumerModel(String serviceName,ReferenceConfig metadata, Object proxyObject, Method[] methods) {
        this.serviceName = serviceName;
        this.metadata = metadata;
        this.proxyObject = proxyObject;

        if (proxyObject != null) {
            for (Method method : methods) {
                methodModels.put(method, new ConsumerMethodModel(method, metadata));
            }
        }
    }

    /**
     * 返回消费服务对应的元数据
     *
     * @return 服务元数据
     */
    public ReferenceConfig getMetadata() {
        return metadata;
    }

    public Object getProxyObject() {
        return proxyObject;
    }

    /**
     * 根据用户调用的方法对象，返回消费的方法模型
     *
     * @param method 反射的方法对象
     * @return 方法模型
     */
    public ConsumerMethodModel getMethodModel(Method method) {
        return methodModels.get(method);
    }

    /**
     * 返回当前服务下的所有方法模型
     *
     * @return 方法列表，不会为空
     */
    public List<ConsumerMethodModel> getAllMethods() {
        return new ArrayList<ConsumerMethodModel>(methodModels.values());
    }

    public String getServiceName() {
        return serviceName;
    }
}
