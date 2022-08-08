/**
 * Alipay.com Inc. Copyright (c) 2004-2022 All Rights Reserved.
 */
package org.apache.dubbo.common.concurrent;

import org.apache.dubbo.common.threadpool.MemorySafeLinkedBlockingQueue;

/**
 * A handler for rejected element that discards the oldest element.
 */
public class DiscardOldestPolicy<E> implements Rejector<E> {

    @Override
    public void reject(final E e, final MemorySafeLinkedBlockingQueue<E> queue) {
        queue.poll();
        queue.offer(e);
    }
}
