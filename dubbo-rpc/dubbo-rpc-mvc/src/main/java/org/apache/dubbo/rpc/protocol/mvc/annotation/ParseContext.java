package org.apache.dubbo.rpc.protocol.mvc.annotation;

import org.apache.dubbo.rpc.protocol.mvc.request.ServletRequestFacade;

import java.util.Arrays;
import java.util.List;

public class ParseContext {


    private ServletRequestFacade requestFacade;

    private List<Object> args;

    private List<ArgInfo> argInfos;

    private Object response;
    private Object request;


    public ParseContext() {
    }

    public ParseContext(ServletRequestFacade request, List<ArgInfo> argInfos) {
        this.requestFacade = request;
        this.argInfos = argInfos;
        args = createDefaultListArgs(argInfos.size());
    }

    private List<Object> createDefaultListArgs(int size) {
        return Arrays.asList(new Object[size]);
    }

    public ParseContext(ServletRequestFacade request) {
        this.requestFacade = request;
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

    public ServletRequestFacade getRequestFacade() {
        return requestFacade;
    }

    public void setValueByIndex(int index, Object value) {

        this.args.set(index, value);
    }

    public boolean isResponseArg(Class response) {
        return response.isAssignableFrom(this.response.getClass());
    }

    public boolean isRequestArg(Class request) {
        return request.isAssignableFrom(this.request.getClass());
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }


}
