package org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider;


import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.rpc.protocol.rest.annotation.BaseParseContext;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

import java.util.Arrays;
import java.util.List;

public class ProviderParseContext extends BaseParseContext {


    private RequestFacade requestFacade;
    private Object response;
    private Object request;


    public ProviderParseContext() {
    }

    public ProviderParseContext(RequestFacade request, List<ArgInfo> argInfos) {
        this.requestFacade = request;
        this.argInfos = argInfos;
        args = createDefaultListArgs(argInfos.size());
    }

    private List<Object> createDefaultListArgs(int size) {
        return Arrays.asList(new Object[size]);
    }

    public ProviderParseContext(RequestFacade request) {
        this.requestFacade = request;
    }

    public RequestFacade getRequestFacade() {
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

    public String getPathVariable(int urlSplitIndex) {

        String[] split = getRequestFacade().getRequestURI().split("/");

        return split[urlSplitIndex];

    }


}
