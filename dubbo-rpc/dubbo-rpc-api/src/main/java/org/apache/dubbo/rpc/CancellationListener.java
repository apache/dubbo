package org.apache.dubbo.rpc;

/**
 * A listener notified on context cancellation.
 */
public interface CancellationListener {
    /**
     * Notifies that a context was cancelled.
     *
     * @param context the newly cancelled context.
     */
    void cancelled(CancellableContext context);
}