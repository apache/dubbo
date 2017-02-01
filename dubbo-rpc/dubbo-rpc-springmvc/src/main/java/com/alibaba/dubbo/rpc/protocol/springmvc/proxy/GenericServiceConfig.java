package com.alibaba.dubbo.rpc.protocol.springmvc.proxy;

import com.alibaba.dubbo.common.utils.StringUtils;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by wuyu on 2016/7/7.
 */
public class GenericServiceConfig implements Serializable {

    private static final long serialVersionUID = 1064223171940612201L;

    //兼容 jsonrpc 如果携带次参数 将以jsonrpc 格式返回
    private String jsonrpc;

    //兼容 jsonrpc
    private String id;

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
    private String[] params;

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


    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

    public GenericServiceConfig() {
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

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
