package com.alibaba.dubbo.rpc.protocol.springmvc.proxy;

import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ASMJavaBeanDeserializer;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.util.TypeUtils;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2016/7/14.
 */
public class ProxyServiceImpl implements ProxyService, DisposableBean {


    private Map<String, GenericService> genericServiceMap = new ConcurrentHashMap<String, GenericService>();

    private Map<String, Class> clazzCache = new ConcurrentHashMap<>();


    /**
     * http://localhost:8080/proxy/
     * POST,PUT,DELETE
     * 调用示例
     * {
     * "service":"com.alibaba.dubbo.demo.DemoService",
     * "method":"sayHello",
     * "group":"defaultGroup",//可以不写
     * "version":"1.0" ,//可以不写
     * "argsType":["java.lang.String"],
     * "args":["wuyu"]
     * }
     *
     * @param config
     * @return
     */
    @ResponseBody
    public Object proxy(@RequestBody GenericServiceConfig config) {
        if (config.getService() == null || config.getMethod() == null) {
            throw new IllegalArgumentException(config.toString() + " Miss required parameter! ");
        }

        try {
            Method method = getMethod(config.getService(), config.getMethod(), config.getArgsType());
            Object[] args = convertArgs(method, config.getArgs());
            return genericService(config).$invoke(method.getName(), config.getArgsType(), args);
        } catch (Exception e) {
            throw new RpcException(e);
        }

    }


    public Object[] convertArgs(Method method, String[] args) throws IOException {
        Object[] convertArgs = new Object[args.length];
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        for (int i = 0; i < genericParameterTypes.length; i++) {
            JavaType javaType = TypeFactory.defaultInstance().constructType(genericParameterTypes[i]);
            if (javaType.isCollectionLikeType() || javaType.isArrayType()) {
                convertArgs[i] = JSON.parseArray(args[i], javaType.getContentType().getRawClass());
            } else if (javaType.isMapLikeType()) {
                convertArgs[i] = JSON.parseObject(args[i], javaType.getRawClass());
            } else {
                ParserConfig parserConfig = ParserConfig.getGlobalInstance();
                if (parserConfig.getDeserializer(javaType.getRawClass()) instanceof ASMJavaBeanDeserializer) {
                    convertArgs[i] = JSON.parseObject(args[i], javaType.getRawClass());
                } else {
                    convertArgs[i] = TypeUtils.cast(args[i], javaType.getRawClass(), ParserConfig.getGlobalInstance());
                }
            }
        }

        return convertArgs;
    }

    public Method getMethod(String serviceName, String methodName, String[] argsType) throws ClassNotFoundException, NoSuchMethodException {
        Class serviceClazz = getClazz(serviceName);
        Class[] clazzTypes = new Class[argsType.length];
        for (int i = 0; i < argsType.length; i++) {
            clazzTypes[i] = getClazz(argsType[i]);
        }
        return serviceClazz.getMethod(methodName, clazzTypes);
    }

    public Class getClazz(String clazzName) throws ClassNotFoundException {
        Class aClass = clazzCache.get(clazzName);
        if (aClass != null) {
            return aClass;
        }

        aClass = Class.forName(clazzName);
        clazzCache.put(clazzName, aClass);
        return aClass;
    }

    protected GenericService genericService(GenericServiceConfig config) {
        String key = sliceKey(config);
        GenericService genericService = genericServiceMap.get(key);
        if (genericService != null) {
            return genericService;
        }
        ApplicationContext springContext = ServiceBean.getSpringContext();
        ReferenceBean<GenericService> reference = new ReferenceBean<GenericService>(); // 该实例很重量，里面封装了所有与注册中心及服务提供方连接，请缓存
        reference.setApplicationContext(springContext);
        reference.setInterface(config.getService()); // 弱类型接口名
        if (config.getVersion() != null && !config.getVersion().equals("0.0.0")) {
            reference.setVersion(config.getVersion());
        }
        if (config.getGroup() != null && (!config.getGroup().equalsIgnoreCase("defaultGroup"))) {
            reference.setGroup(config.getGroup());
        }

        String[] registries = springContext.getBeanNamesForType(RegistryConfig.class);
        for (String registry : registries) {
            reference.setRegistry(springContext.getBean(registry, RegistryConfig.class));
        }
        reference.setGeneric(true); // 声明为泛化接口
        genericService = reference.get();
        genericServiceMap.put(key, genericService);
        return genericService; // 用com.alibaba.dubbo.rpc.service.GenericService可以替代所有接口引用
    }

    private String sliceKey(GenericServiceConfig config) {
        return "/" + config.getGroup() + "/" + config.getVersion() + "/" + config.getService();
    }

    @Override
    public void destroy() throws Exception {
        genericServiceMap.clear();
    }
}
