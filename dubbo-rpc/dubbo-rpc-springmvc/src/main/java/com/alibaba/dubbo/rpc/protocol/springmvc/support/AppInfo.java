package com.alibaba.dubbo.rpc.protocol.springmvc.support;

import java.util.Set;

/**
 * Created by wuyu on 2017/1/11.
 */
public class AppInfo {

    private String name;

    private Set<String> serverList;

    private String schema;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getServerList() {
        return serverList;
    }

    public void setServerList(Set<String> serverList) {
        this.serverList = serverList;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String serverListToString(String delimiter) {
        String servers = "";
        for (String server : serverList) {
            servers += server + ",";
        }
        return servers;
    }
}
