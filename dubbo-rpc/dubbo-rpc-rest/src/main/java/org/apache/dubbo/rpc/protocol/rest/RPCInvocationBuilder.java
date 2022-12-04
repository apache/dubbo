package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParamParserManager;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParseContext;
import org.apache.dubbo.rpc.protocol.mvc.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.mvc.request.RequestFacadeFactory;
import org.apache.dubbo.rpc.protocol.mvc.request.ServletRequestFacade;

import java.util.ArrayList;
import java.util.List;


public class RPCInvocationBuilder {


    private static final ParamParserManager paramParser = new ParamParserManager();


    public static RpcInvocation build(Object request, Object response) {


        RpcInvocation rpcInvocation = createBaseRpcInvocation(request);

        ParseContext parseContext = createParseContext(request, response, rpcInvocation);

        Object[] args = paramParser.providerParamParse(parseContext);
        rpcInvocation.setArguments(args);

        return rpcInvocation;

    }

    private static ParseContext createParseContext(Object request, Object response, RpcInvocation rpcInvocation) {
        ParseContext parseContext = new ParseContext(RequestFacadeFactory.createRequestFacade(request));
        parseContext.setResponse(response);
        parseContext.setRequest(request);

        // TODO create  List<ArgInfo> according to consumer method definition related header

        List<ArgInfo> argInfos = new ArrayList<>();

        //
        parseContext.setArgInfos(argInfos);


        return parseContext;
    }

    private static RpcInvocation createBaseRpcInvocation(Object servletRequest) {
        RpcInvocation rpcInvocation = new RpcInvocation();
        ServletRequestFacade request = RequestFacadeFactory.createRequestFacade(servletRequest);

        int localPort = request.getLocalPort();
        String localAddr = request.getLocalAddr();
        int remotePort = request.getRemotePort();
        String remoteAddr = request.getRemoteAddr();

        String HOST = request.getHeader(RestConstant.HOST);
        String GROUP = request.getHeader(RestConstant.GROUP);
        String METHOD = request.getHeader(RestConstant.METHOD);
        String PARAMETER_TYPES_DESC = request.getHeader(RestConstant.PARAMETER_TYPES_DESC);
        String PATH = request.getHeader(RestConstant.PATH);
        String VERSION = request.getHeader(RestConstant.VERSION);

        rpcInvocation.setMethodName(METHOD);
        rpcInvocation.setAttachment(RestConstant.GROUP, GROUP);
        rpcInvocation.setAttachment(RestConstant.METHOD, METHOD);
        rpcInvocation.setAttachment(RestConstant.PARAMETER_TYPES_DESC, PARAMETER_TYPES_DESC);
        rpcInvocation.setAttachment(RestConstant.PATH, PATH);
        rpcInvocation.setAttachment(RestConstant.VERSION, VERSION);
        rpcInvocation.setAttachment(RestConstant.HOST, HOST);
        rpcInvocation.setAttachment(RestConstant.REMOTE_ADDR, remoteAddr);
        rpcInvocation.setAttachment(RestConstant.LOCAL_ADDR, localAddr);
        rpcInvocation.setAttachment(RestConstant.REMOTE_PORT, remotePort);
        rpcInvocation.setAttachment(RestConstant.LOCAL_PORT, localPort);
        // TODO set path,version,group and so on
        return rpcInvocation;
    }


}
