package org.apache.dubbo.rpc.protocol.mvc.annotation.provider;

import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParamParserManager;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParseContext;
import org.apache.dubbo.rpc.protocol.mvc.request.RequestFacadeFactory;

import java.util.ArrayList;
import java.util.List;


public class RPCInvocationBuilder {


    private static final ParamParserManager paramParser = new ParamParserManager();


    public static RpcInvocation build(Object request) {


        RpcInvocation rpcInvocation = createBaseRpcInvocation(request);

        ParseContext parseContext = createParseContext(request, rpcInvocation);

        Object[] args = paramParser.providerParamParse(parseContext);
        rpcInvocation.setArguments(args);

        return rpcInvocation;

    }

    private static ParseContext createParseContext(Object request, RpcInvocation rpcInvocation) {
        ParseContext parseContext = new ParseContext(RequestFacadeFactory.createRequestFacade(request));

        // TODO create  List<ArgInfo> according to consumer method definition related header

        List<ArgInfo> argInfos = new ArrayList<>();

        //
        parseContext.setArgInfos(argInfos);


        return parseContext;
    }

    private static RpcInvocation createBaseRpcInvocation(Object request) {
        RpcInvocation rpcInvocation = new RpcInvocation();

        // TODO set path,version,group and so on
        return rpcInvocation;
    }


}
