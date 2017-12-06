package com.alibaba.dubbo.config.model;

import com.alibaba.dubbo.config.ServiceConfig;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author qinliujie
 * @date 2017/11/22
 */
public class ProviderModel {
    private final String serviceName;
    private final Object serviceInstance;
    private final ServiceConfig metadata;
    private final Map<String, List<ProviderMethodModel>> methods = new HashMap<String, List<ProviderMethodModel>>();

    public ProviderModel(String serviceName, ServiceConfig metadata, Object serviceInstance) {
        if (null == serviceInstance) {
            throw new IllegalArgumentException("Servie[" + serviceName + "]Target is NULL.");
        }

        this.serviceName = serviceName;
        this.metadata = metadata;
        this.serviceInstance = serviceInstance;

        initMethod();
    }


    public String getServiceName() {
        return serviceName;
    }

    public ServiceConfig getMetadata() {
        return metadata;
    }

    public Object getServiceInstance() {
        return serviceInstance;
    }

    public List<ProviderMethodModel> getAllMethods() {
        List<ProviderMethodModel> result = new ArrayList<ProviderMethodModel>();
        for (List<ProviderMethodModel> models : methods.values()) {
            result.addAll(models);
        }
        return result;
    }

    public ProviderMethodModel getMethodModel(String methodName, String[] argTypes) {
        List<ProviderMethodModel> methodModels = methods.get(methodName);
        if (methodModels != null) {
            for (ProviderMethodModel methodModel : methodModels) {
                if (Arrays.equals(argTypes, methodModel.getMethodArgTypes())) {
                    return methodModel;
                }
            }
        }
        return null;
    }

    private void initMethod() {
        Method[] methodsToExport = null;
        methodsToExport = metadata.getInterfaceClass().getMethods();

        for (Method method : methodsToExport) {
            method.setAccessible(true);

            List<ProviderMethodModel> methodModels = methods.get(method.getName());
            if (methodModels == null) {
                methodModels = new ArrayList<ProviderMethodModel>(1);
                methods.put(method.getName(), methodModels);
            }
            methodModels.add(new ProviderMethodModel(method, serviceName));
        }
    }

}
