package org.apache.dubbo.rpc.protocol.tri.frame;

import io.netty.channel.ChannelFuture;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author owen.cai
 * @create_date 2022/9/30
 * @alter_author
 * @alter_date
 */
public class MessageFramer implements Framer {
    private final WriteQueue writeQueue;
    private boolean closed;
    private final Queue<DataQueueCommand> queue = new ConcurrentLinkedQueue<>();

    public MessageFramer(WriteQueue writeQueue) {
        this.writeQueue = writeQueue;
    }

    @Override
    public void addDataCmd(DataQueueCommand cmd) {
        queue.add(cmd);
    }

    @Override
    public void close() {
        if(!closed) {
            closed = true;
            commitToSink(true, true);
        }
    }

    private void commitToSink(boolean endOfStream, boolean flush) {
        for (DataQueueCommand dataQueueCommand : queue) {
            writeQueue.enqueueSoon(dataQueueCommand, false);
        }
        writeQueue.scheduleFlush();
    }
}
