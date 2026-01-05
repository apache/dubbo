package org.apache.dubbo.common.stream;

public interface ClientCallStreamObserver<Req> extends CallStreamObserver<Req> {


    void disableAutoRequestWithInitial(int request);

}
