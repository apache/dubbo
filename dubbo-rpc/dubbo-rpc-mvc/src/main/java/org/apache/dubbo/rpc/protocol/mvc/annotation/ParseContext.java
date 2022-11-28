package org.apache.dubbo.rpc.protocol.mvc.annotation;

import org.apache.dubbo.rpc.protocol.mvc.request.ServletRequestFacade;

import java.util.Arrays;
import java.util.List;

public class ParseContext {


    private ServletRequestFacade request;

    private List<Object> args;

    private List<ArgInfo> argInfos;


    public ParseContext() {
    }

    public ParseContext(ServletRequestFacade request, List<ArgInfo> argInfos) {
        this.request = request;
        this.argInfos = argInfos;
        args = createDefaultListArgs(argInfos.size());
    }

    private List<Object> createDefaultListArgs(int size) {
        return Arrays.asList(new Object[size]);
    }

    public ParseContext(ServletRequestFacade request) {
        this.request = request;
    }

    public List<ArgInfo> getArgInfos() {
        return argInfos;
    }

    public void setArgInfos(List<ArgInfo> argInfos) {
        this.argInfos = argInfos;
        createDefaultListArgs(argInfos.size());
    }


    public ArgInfo getArgInfoByIndex(int index) {
        return getArgInfos().get(index);
    }

    public List<Object> getArgs() {
        return args;
    }

    public void setArgs(List<Object> args) {
        this.args = args;
    }

    public ServletRequestFacade getRequest() {
        return request;
    }

    public void setValueByIndex(int index, Object value) {

        this.args.set(index, value);
    }
}
