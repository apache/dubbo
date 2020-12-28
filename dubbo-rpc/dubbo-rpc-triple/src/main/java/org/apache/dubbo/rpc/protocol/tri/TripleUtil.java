package org.apache.dubbo.rpc.protocol.tri;

public class TripleUtil {

    /**
     * must starts from application/grpc
     */
    public static boolean supportContentType(String contentType) {
        if (contentType == null) {
            return false;
        }

        return contentType.startsWith(TripleConstant.APPLICATION_GRPC);
    }
}
