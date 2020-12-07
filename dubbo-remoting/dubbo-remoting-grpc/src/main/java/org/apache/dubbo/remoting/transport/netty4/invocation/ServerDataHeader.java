package org.apache.dubbo.remoting.transport.netty4.invocation;


public class ServerDataHeader extends DataHeader {
    public ServerDataHeader(Object header) {
        super(header);
    }
    //public final GRpcServerSubscriber subscriber;
    //
    //public ServerDataHeader(Object header, boolean endStream, GRpcServerSubscriber subscriber) {
    //    super(header, endStream);
    //    this.subscriber = subscriber;
    //}
    //
    //public ServerDataHeader(Object header, int streamId, boolean endStream, GRpcServerSubscriber subscriber) {
    //    super(header, streamId, endStream);
    //    this.subscriber = subscriber;
    //}
    //
    //public ServerDataHeader(Object header, GRpcServerSubscriber subscriber) {
    //    super(header);
    //    this.subscriber = subscriber;
    //}
}
