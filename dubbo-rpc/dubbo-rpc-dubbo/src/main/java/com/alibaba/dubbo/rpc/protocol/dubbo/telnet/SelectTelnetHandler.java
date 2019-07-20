package com.alibaba.dubbo.rpc.protocol.dubbo.telnet;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.remoting.telnet.support.Help;

import java.lang.reflect.Method;
import java.util.List;
/**
 * SelectTelnetHandler
 */
@Activate
@Help(parameter = "[index]", summary = "Select the index of the method you want to invoke.",
        detail = "Select the index of the method you want to invoke.")
public class SelectTelnetHandler implements TelnetHandler {
    static final String SELECT_METHOD_KEY = "telnet.select.method";
    static final String SELECT_KEY = "telnet.select";

    private InvokeTelnetHandler invokeTelnetHandler = new InvokeTelnetHandler();

    @Override
    @SuppressWarnings("unchecked")
    public String telnet(Channel channel, String message) {
        if (message == null || message.length() == 0) {
            return "Please input the index of the method you want to invoke, eg: \r\n select 1";
        }
        List<Method> methodList = (List<Method>) channel.getAttribute(InvokeTelnetHandler.INVOKE_METHOD_LIST_KEY);
        if (CollectionUtils.isEmpty(methodList)) {
            return "Please use the invoke command first.";
        }
        if (!StringUtils.isInteger(message) || Integer.parseInt(message) < 1 || Integer.parseInt(message) > methodList.size()) {
            return "Illegal index ,please input select 1~" + methodList.size();
        }
        Method method = methodList.get(Integer.parseInt(message));
        channel.setAttribute(SELECT_METHOD_KEY, method);
        channel.setAttribute(SELECT_KEY, Boolean.TRUE);
        String invokeMessage = (String) channel.getAttribute(InvokeTelnetHandler.INVOKE_MESSAGE_KEY);
        return invokeTelnetHandler.telnet(channel, invokeMessage);
    }
}