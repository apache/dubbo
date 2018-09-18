package org.apache.dubbo.servicedata.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author cvictory ON 2018/9/18
 */
public class ServiceDescriptor {
    private String name;
    private String codeSource;
    private List<MethodDescriptor> methodDescriptors = new ArrayList<>();
    /**
     * Primitive type and String will not be stored.
     */
    private Map<String, TypeDescriptor> types = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCodeSource() {
        return codeSource;
    }

    public void setCodeSource(String codeSource) {
        this.codeSource = codeSource;
    }

    public List<MethodDescriptor> getMethodDescriptors() {
        return methodDescriptors;
    }

    public void setMethodDescriptors(List<MethodDescriptor> methodDescriptors) {
        this.methodDescriptors = methodDescriptors;
    }

    public Map<String, TypeDescriptor> getTypes() {
        return types;
    }

    public void setTypes(Map<String, TypeDescriptor> types) {
        this.types = types;
    }
}
