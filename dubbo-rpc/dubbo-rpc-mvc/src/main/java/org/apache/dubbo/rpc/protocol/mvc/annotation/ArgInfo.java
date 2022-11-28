package org.apache.dubbo.rpc.protocol.mvc.annotation;


public abstract class ArgInfo {
    /**
     * method arg index 0,1,2,3
     */
    private int index;
    /**
     * method annotation name or name
     */
    private String annoNameAttribute;

    /**
     * param annotation type
     */
    private Class paramAnno;

    /**
     *  param Type
     */
    private Class paramType;

    public ArgInfo(int index, String name, Class paramType) {
        this.index = index;
        this.annoNameAttribute = name;
        this.paramAnno = paramType;
    }

    public ArgInfo() {
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getAnnoNameAttribute() {
        return annoNameAttribute;
    }

    public void setAnnoNameAttribute(String annoNameAttribute) {
        this.annoNameAttribute = annoNameAttribute;
    }

    public Class getParamAnno() {
        return paramAnno;
    }

    public void setParamAnno(Class paramAnno) {
        this.paramAnno = paramAnno;
    }

    public Class getParamType() {
        return paramType;
    }

    public void setParamType(Class paramType) {
        this.paramType = paramType;
    }
}
