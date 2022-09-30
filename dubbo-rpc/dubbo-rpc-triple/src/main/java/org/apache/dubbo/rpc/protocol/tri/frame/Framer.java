package org.apache.dubbo.rpc.protocol.tri.frame;

import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;

/**
 * @author owen.cai
 * @create_date 2022/9/30
 * @alter_author
 * @alter_date
 */
public interface Framer {
    void addDataCmd(DataQueueCommand cmd);

    void close();
}
