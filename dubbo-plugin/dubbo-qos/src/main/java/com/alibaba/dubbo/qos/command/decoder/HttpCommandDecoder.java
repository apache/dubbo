package com.alibaba.dubbo.qos.command.decoder;

import com.alibaba.dubbo.qos.command.CommandContext;
import com.alibaba.dubbo.qos.command.CommandContextFactory;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author qinliujie
 * @date 2017/11/17
 */
public class HttpCommandDecoder {
    public static CommandContext decode(HttpRequest request) {
        CommandContext commandContext = null;
        if (request != null) {
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
            String path = queryStringDecoder.path();
            String[] array = path.split("/");
            if (array.length == 2) {
                String name = array[1];

                // Get请求和Post请求分开处理
                // Get看url的Path
                // Post看请求正文
                if (request.getMethod() == HttpMethod.GET) {
                    if (queryStringDecoder.parameters().isEmpty()) {
                        commandContext = CommandContextFactory.newInstance(name);
                        commandContext.setHttp(true);
                    } else {
                        List<String> valueList = new ArrayList<String>();
                        for (List<String> values : queryStringDecoder.parameters().values()) {
                            valueList.addAll(values);
                        }
                        commandContext = CommandContextFactory.newInstance(name, valueList.toArray(new String[]{}),true);
                    }
                } else if (request.getMethod() == HttpMethod.POST) {
                    HttpPostRequestDecoder httpPostRequestDecoder = new HttpPostRequestDecoder(request);
                    List<String> valueList = new ArrayList<String>();
                    for (InterfaceHttpData interfaceHttpData : httpPostRequestDecoder.getBodyHttpDatas()) {
                        if (interfaceHttpData.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                            Attribute attribute = (Attribute) interfaceHttpData;
                            try {
                                valueList.add(attribute.getValue());
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                    if (valueList.isEmpty()) {
                        commandContext = CommandContextFactory.newInstance(name);
                        commandContext.setHttp(true);
                    } else {
                        commandContext = CommandContextFactory.newInstance(name, valueList.toArray(new String[]{}),true);
                    }
                }
            }
        }

        return commandContext;
    }
}
