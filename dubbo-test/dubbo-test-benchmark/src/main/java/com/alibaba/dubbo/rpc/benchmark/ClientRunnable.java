/**
 * nfs-rpc
 * Apache License
 * <p>
 * http://code.google.com/p/nfs-rpc (c) 2011
 */
package com.alibaba.dubbo.rpc.benchmark;

import java.util.List;

public interface ClientRunnable extends Runnable {

    public List<long[]> getResults();

}
