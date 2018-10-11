package org.apache.dubbo.servicedata.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *  2018/9/18
 */
public class TypeDescriptor {

    private String type;
    private boolean custom;
    private Map<String, TypeDescriptor> properties = new HashMap<>();

    public TypeDescriptor(String type){
        this.type = type;
    }

    public TypeDescriptor(String type, boolean custom){
        this.type = type;
        this.custom = custom;
    }

    public static TypeDescriptor simplifyTypeDescriptor(TypeDescriptor typeDescriptor){
        return new TypeDescriptor(typeDescriptor.getType(), typeDescriptor.isCustom());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, TypeDescriptor> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, TypeDescriptor> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeDescriptor)) return false;
        TypeDescriptor that = (TypeDescriptor) o;
        return Objects.equals(getType(), that.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType());
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }
}
