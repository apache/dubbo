package org.apache.dubbo.rpc.protocol.rest.annotation;


import org.apache.dubbo.metadata.rest.ArgInfo;

import java.util.List;

public class BaseParseContext {


    protected List<Object> args;

    protected List<ArgInfo> argInfos;


    public List<Object> getArgs() {
        return args;
    }

    public void setArgs(List<Object> args) {
        this.args = args;
    }

    public List<ArgInfo> getArgInfos() {
        return argInfos;
    }

    public void setArgInfos(List<ArgInfo> argInfos) {
        this.argInfos = argInfos;
    }

    public ArgInfo getArgInfoByIndex(int index) {
        return getArgInfos().get(index);
    }
}
