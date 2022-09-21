package org.apache.dubbo.errorcode.model;

/**
 * Represents a Field definition in a class.
 */
public class FieldDefinition {
    private String containerClass;
    private String fieldType;
    private String fieldName;

    public FieldDefinition(String containerClass, String fieldType, String fieldName) {
        this.containerClass = containerClass;
        this.fieldType = fieldType;
        this.fieldName = fieldName;
    }

    public FieldDefinition() {
    }

    public String getContainerClass() {
        return this.containerClass;
    }

    public String getFieldType() {
        return this.fieldType;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public void setContainerClass(String containerClass) {
        this.containerClass = containerClass;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldDefinition that = (FieldDefinition) o;

        if (!containerClass.equals(that.containerClass)) return false;
        if (!fieldType.equals(that.fieldType)) return false;
        return fieldName.equals(that.fieldName);
    }

    @Override
    public int hashCode() {
        int result = containerClass.hashCode();
        result = 31 * result + fieldType.hashCode();
        result = 31 * result + fieldName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "FieldDefinition{" +
            "containerClass='" + containerClass + '\'' +
            ", fieldType='" + fieldType + '\'' +
            ", fieldName='" + fieldName + '\'' +
            '}';
    }
}
