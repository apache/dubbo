package org.apache.dubbo.rpc;

public class RpcServerContextAttachment extends RpcContextAttachment{
    private static final RpcContextAttachment AGENT_SERVER_CONTEXT = new RpcContextAttachment() {
        @Override
        public RpcContextAttachment setObjectAttachment(String key, Object value) {
            if (value == null) {
                attachments.remove(key);
            } else {
                RpcContext.getServerResponseContext().setAttachment(key, value);
                attachments.put(key, value);
            }
            return this;
        }
    };

    /**
     * get server side context. ( A <-- B , in B side)
     *
     * @return server context
     */
    public static RpcContextAttachment getServerContext() {
        return (RpcContextAttachment) AGENT_SERVER_CONTEXT;
    }
}
