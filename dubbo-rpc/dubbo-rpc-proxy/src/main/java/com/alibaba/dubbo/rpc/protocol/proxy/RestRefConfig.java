package com.alibaba.dubbo.rpc.protocol.proxy;

import com.alibaba.dubbo.common.utils.StringUtils;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by wuyu on 2017/2/2.
 */
public class RestRefConfig implements Serializable{

    private static final long serialVersionUID = 1064223171940612201L;

    //服务名
    private String service;

    //方法
    private String method;

    //组名
    private String group;

    //版本
    private String version;

    //参数类型
    private String[] paramsType;

    //参数
    private Object[] params;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        if (StringUtils.isBlank(group)) {
            return;
        }
        this.group = group;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (StringUtils.isBlank(version)) {
            return;
        }
        this.version = version;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public Object[] getParams() {
        return params;
    }

    public RestRefConfig() {
    }

    public RestRefConfig(String group, String version, String service, String method, Object[] params, String[] paramsType) {
        this.group = group;
        this.version = version;
        this.service = service;
        this.method = method;
        this.params = params;
        this.paramsType = paramsType;
    }

    public RestRefConfig(String service, String method, Object[] params ,String[] paramsType) {
        this.service = service;
        this.method = method;
        this.params = params;
        this.paramsType = paramsType;
    }

    public String[] getParamsType() {
        return paramsType;
    }

    public void setParamsType(String[] paramsType) {
        this.paramsType = paramsType;
    }

    @Override
    public String toString() {
        return "GenericServiceConfig{" +
                "service='" + service + '\'' +
                ", method='" + method + '\'' +
                ", group='" + group + '\'' +
                ", version='" + version + '\'' +
                ", paramsType=" + Arrays.toString(paramsType) +
                ", params=" + Arrays.toString(params) +
                '}';
    }
}
