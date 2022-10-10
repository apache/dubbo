package org.apache.dubbo.rpc.protocol.tri.frame;

import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.EndStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.transport.WriteQueue;

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
        int size = queue.size();
        if(size > 0) {
            if(size == 1) {
                DataQueueCommand poll = queue.poll();
                poll.setEndStream(true);
                writeQueue.enqueueSoon(queue.poll(), true);
            }
            else {
                for (DataQueueCommand dataQueueCommand : queue) {
                    writeQueue.enqueueSoon(dataQueueCommand, false);
                }
                EndStreamQueueCommand endStreamQueueCommand = new EndStreamQueueCommand();
                writeQueue.enqueueSoon(endStreamQueueCommand, true);
            }
        }
    }
}
