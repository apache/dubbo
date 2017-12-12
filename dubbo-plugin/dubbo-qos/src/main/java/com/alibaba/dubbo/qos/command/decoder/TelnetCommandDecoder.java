package com.alibaba.dubbo.qos.command.decoder;

import com.alibaba.dubbo.qos.command.CommandContext;
import com.alibaba.dubbo.qos.command.CommandContextFactory;

import org.apache.commons.lang3.StringUtils;

/**
 * @author qinliujie
 * @date 2017/11/17
 */
public class TelnetCommandDecoder {
    public static final CommandContext decode(String str) {
        CommandContext commandContext = null;
        if (StringUtils.isNotBlank(str)) {
            String[] array = str.split("(?<![\\\\]) ");
            if (array.length > 0) {
                String name = array[0];
                String[] targetArgs = new String[array.length - 1];
                System.arraycopy(array, 1, targetArgs, 0, array.length - 1);
                commandContext = CommandContextFactory.newInstance( name, targetArgs,false);
                commandContext.setOrginRequest(str);
            }
        }

        return commandContext;
    }

}
