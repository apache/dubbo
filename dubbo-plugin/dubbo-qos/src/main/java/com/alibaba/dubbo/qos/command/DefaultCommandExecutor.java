package com.alibaba.dubbo.qos.command;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 * @author qinliujie
 * @date 2017/11/17
 */
public class DefaultCommandExecutor implements CommandExecutor {
    @Override
    public String execute(CommandContext commandContext) throws NoSuchCommandException {
        BaseCommand command = null;
        try {
            command = ExtensionLoader.getExtensionLoader(BaseCommand.class).getExtension(commandContext.getCommandName());
        } catch (Throwable throwable) {
                //can't find command
        }
        if (command == null) {
            throw new NoSuchCommandException(commandContext.getCommandName());
        }
        return command.execute(commandContext, commandContext.getArgs());
    }
}
