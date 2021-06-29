package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;


@Cmd(name = "cd", summary = "Change default service.", example = {
    "cd [service]"
})
public class ChangeTelnet implements BaseCommand {

    public static final AttributeKey<String> SERVICE_KEY = AttributeKey.valueOf("telnet.service");

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        Channel channel = commandContext.getRemote();

        if (args == null || args.length < 1) {
            return "Please input service name, eg: \r\ncd XxxService\r\ncd com.xxx.XxxService";
        }
        String message = args[0];
        StringBuilder buf = new StringBuilder();
        if ("/".equals(message) || "..".equals(message)) {
            String service = channel.attr(SERVICE_KEY).getAndRemove();
            buf.append("Cancelled default service ").append(service).append(".");
        } else {
            boolean found = false;
            for (Exporter<?> exporter : DubboProtocol.getDubboProtocol().getExporters()) {
                if (message.equals(exporter.getInvoker().getInterface().getSimpleName())
                    || message.equals(exporter.getInvoker().getInterface().getName())
                    || message.equals(exporter.getInvoker().getUrl().getPath())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                channel.attr(SERVICE_KEY).set(message);
                buf.append("Used the ").append(message).append(" as default.\r\nYou can cancel default service by command: cd /");
            } else {
                buf.append("No such service ").append(message);
            }
        }
        return buf.toString();
    }
}
