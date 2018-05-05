package com.alibaba.dubbo.common.threadlocal;

import com.alibaba.dubbo.common.utils.NamedThreadFactory;

/**
 * NamedInternalThreadFactory
 * This is a threadFactory which produce {@link InternalThread}
 *
 * @author xiuyuhang [xiuyuhang]
 * @since 2018-05-05
 */
public class NamedInternalThreadFactory extends NamedThreadFactory {

    public NamedInternalThreadFactory() {
        super();
    }

    public NamedInternalThreadFactory(String prefix) {
        super(prefix, false);
    }

    public NamedInternalThreadFactory(String prefix, boolean daemon) {
        super(prefix, daemon);
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = mPrefix + mThreadNum.getAndIncrement();
        InternalThread ret = new InternalThread(mGroup, runnable, name, 0);
        ret.setDaemon(mDaemon);
        return ret;
    }
}
