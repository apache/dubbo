package com.alibaba.dubbo.rpc.protocol.xml;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/2/4.
 */
public class XmlRpcHandlerMappingImpl extends PropertyHandlerMapping {

    private Map<String, Object> beanMap = new ConcurrentHashMap<>();

    @Override
    protected XmlRpcHandler newXmlRpcHandler(Class pClass, Method[] pMethods) throws XmlRpcException {
        Object bean = beanMap.get(pClass.getName());
        RequestProcessorFactoryFactory.RequestProcessorFactory factory = getRequestProcessorFactoryFactory().getRequestProcessorFactory(pClass);
        return new ReflectiveXmlRpcInstanceHandler(this, getTypeConverterFactory(),
                pClass, factory, pMethods, bean);
    }

    @Override
    public void addHandler(String pKey, Class pClass) throws XmlRpcException {
        super.addHandler(pKey, pClass);
    }

    public void addHandler(String pKey, Class pClass, Object bean) throws XmlRpcException {
        beanMap.put(pClass.getName(), bean);
        super.addHandler(pKey, pClass);
    }

}
