package com.alibaba.dubbo.qos.command;

/**
 * @author qinliujie
 * @date 2017/11/17
 */
public interface CommandExecutor {
    /**
     * <pre>
     * 执行一个命令，返回对应命令执行的结果
     * </pre>
     *
     * @param commandContext
     *            命令
     * @return
     */
    String execute(CommandContext commandContext) throws NoSuchCommandException;
}
