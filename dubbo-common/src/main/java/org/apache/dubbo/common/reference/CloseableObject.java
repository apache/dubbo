/*
 * Copyright 2018 Aleksandr Dubinsky
 *
 * Aleksandr Dubinsky licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.dubbo.common.reference;

import org.slf4j.helpers.MessageFormatter;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A base class for closeable objects with support for leak detection but not reference counting.
 *
 * <p> This base class provides the leak detection functionality of {@link ReferenceCountedObject}
 * and {@link ResourceLeakDetector}, but does not offer {@code retain} and {@code release} methods.
 *
 * <p> This class does not offer any functional or performance benefits,
 * and the choice to use it instead of {@code ReferenceCountedObject} is mainly esthetic.
 * The API offered by this class is simpler and more familiar to Java programmers.
 */
public abstract class CloseableObject implements AutoCloseable {

    private static final AtomicIntegerFieldUpdater<CloseableObject> REFERENCE_COUNT_UPDATER = AtomicIntegerFieldUpdater.newUpdater(CloseableObject.class, "referenceCount");

    private volatile int referenceCount = 1;

    private final ResourceReference resourceReference = ResourceLeakDetector.getInstance().tryRegister(this);


    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked when the reference count reaches {@code 0}.
     * This method will not be invoked more than once.
     *
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception.
     *
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code destroy} method throw {@link
     * InterruptedException}.</em>
     * <p>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * <p>
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code ReferenceCountedObject.destroy}
     * method should not throw it.
     */
    protected abstract void
    destroy();


    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     *
     * <p> This method is idempotent. In other words,
     * calling this {@code close} method more than once will not have an effect.
     * The first time this method is called, the object will be destroyed.
     *
     * @see AutoCloseable#close()
     */
    public final @Override
    void
    close() {

        long newCount = REFERENCE_COUNT_UPDATER.decrementAndGet(this);

        if (newCount < 0)
            return;

        try {
            destroy();
        } finally {
            if (resourceReference != null) {
                // Recent versions of the JDK have a nasty habit of prematurely deciding objects are unreachable.
                // see: https://stackoverflow.com/questions/26642153/finalize-called-on-strongly-reachable-object-in-java-8
                // The test ResourceLeakDetectorTest.testConcurrentUsage reproduces this issue on JDK 8
                // if no counter-measures are taken.
                // The method Reference.reachabilityFence offers a solution to this problem.
                // However, besides only being available in Java 9+,
                // it "is designed for use in uncommon situations of premature finalization where using
                // synchronized blocks or methods [is] not possible or do not provide the desired control."
                // Because we just destroyed the object,
                // it is unreasonable that anyone else, anywhere, is hold a lock.
                // Therefore, it seems using a synchronization block is possible here, so we will use one!

                // Java 9:
//                        resourceReference.close();
//                        java.lang.ref.Reference.reachabilityFence(this);

                // Java 8:
                synchronized (this) {
                    resourceReference.unregister();
                }
            }
        }
    }

    /**
     * Records the stack trace for debugging purposes in case this object is detected to have leaked.
     * You must set the {@link ResourceLeakDetector.Level resource leak detector level}
     * to {@link ResourceLeakDetector.Level#DEBUG DEBUG}.
     */
    public final void
    trace() {

        trace(null);
    }

    /**
     * Records the stack trace for debugging purposes in case this object is detected to have leaked.
     * You must set the {@link ResourceLeakDetector.Level resource leak detector level}
     * to {@link ResourceLeakDetector.Level#DEBUG DEBUG}.
     * This method follows the {@link MessageFormatter SLF4J API}.
     *
     * @param format message pattern which will be parsed and formatted
     * @param arg    argument to be substituted in place of the formatting anchor
     * @see ResourceReference#trace(Object)
     */
    public final void
    trace(String format, Object arg) {

        assertNotDestroyed();

        if (resourceReference != null)
            resourceReference.trace(format, arg);
    }

    /**
     * Records the stack trace for debugging purposes in case this object is detected to have leaked.
     * You must set the {@link ResourceLeakDetector.Level resource leak detector level}
     * to {@link ResourceLeakDetector.Level#DEBUG DEBUG}.
     * This method follows the {@link MessageFormatter SLF4J API}.
     *
     * @param format message pattern which will be parsed and formatted
     * @param arg1   argument to be substituted in place of the first formatting anchor
     * @param arg2   argument to be substituted in place of the second formatting anchor
     * @see ResourceReference#trace(Object)
     */
    public final void
    trace(String format, Object arg1, Object arg2) {

        assertNotDestroyed();

        if (resourceReference != null)
            resourceReference.trace(format, arg1, arg2);
    }

    /**
     * Records the stack trace for debugging purposes in case this object is detected to have leaked.
     * You must set the {@link ResourceLeakDetector.Level resource leak detector level}
     * to {@link ResourceLeakDetector.Level#DEBUG DEBUG}.
     * This method follows the {@link MessageFormatter SLF4J API}.
     *
     * @param format   message pattern which will be parsed and formatted
     * @param argArray arguments to be substituted in place of anchors
     * @see ResourceReference#trace(Object)
     */
    public final void
    trace(String format, Object... argArray) {

        assertNotDestroyed();

        if (resourceReference != null)
            resourceReference.trace(format, argArray);
    }

    /**
     * Records the stack trace for debugging purposes in case this object is detected to have leaked.
     * You must set the {@link ResourceLeakDetector.Level resource leak detector level}
     * to {@link ResourceLeakDetector.Level#DEBUG DEBUG}.
     * This method attempts to have minimal performance impact.
     *
     * @param message a string or object whose {@code toString} method will be called
     *                only in the case that a leak is logged and stack traces are enabled
     * @see ResourceReference#trace(Object)
     */
    public final void
    trace(Object message) {

        assertNotDestroyed();

        if (resourceReference != null)
            resourceReference.trace(message);
    }

    /**
     * May be used to assert that the object hasn't been destroyed.
     * In case of concurrency, this method cannot guarantee that the object has not been destroyed.
     * If you need to be sure, implement synchronization in your class and use a flag.
     */
    protected final void
    assertNotDestroyed() {

        if (referenceCount <= 0)
            throw new AssertionError("Trying to use a destroyed object.");
    }
}
