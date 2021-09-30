package org.apache.dubbo.rpc.filter.limiter;

import java.util.Optional;

public interface Limiter {

    interface Listener{
        void onSuccess();

        void onDroped();
    }

    Optional<Listener> acquire();
}
