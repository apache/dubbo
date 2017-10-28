package com.alibaba.dubbo.rpc.support;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;

/**
 * @author ken.lj
 * @date 2017/10/24
 */
public class BlockMyInvoker<T> extends MyInvoker<T> {

    private long blockTime = 100;

    public BlockMyInvoker(URL url, long blockTime) {
        super(url);
        this.blockTime = blockTime;
    }

    public BlockMyInvoker(URL url, boolean hasException, long blockTime) {
        super(url, hasException);
        this.blockTime = blockTime;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        RpcResult result = new RpcResult();
        if (hasException == false) {
            try {
                Thread.sleep(blockTime);
            } catch (InterruptedException e) {
            }
            result.setValue("alibaba");
            return result;
        } else {
            result.setException(new RuntimeException("mocked exception"));
            return result;
        }

    }

    public long getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(long blockTime) {
        this.blockTime = blockTime;
    }
}
