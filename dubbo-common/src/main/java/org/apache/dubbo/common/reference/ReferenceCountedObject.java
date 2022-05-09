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

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * The base class for reference counted objects.
 *
 * <p> Reference counting is a more flexible version of {@code AutoCloseable}.
 * It allows you to do manual, deterministic resource management while letting you pass your resources
 * between objects and methods without deciding on a chain of ownership.
 * Each object has a "reference count" which is initially set to 1.
 * The {@code close} or {@code release} method (they both do the same thing) decrements the reference count.
 * When the reference count reaches 0, the object's {@code destroy} method is called, performing any necessary cleanup.
 * What makes reference counting different from AutoCloseable is the {@code retain} method,
 * which increments the reference count. Call {@code retain} on objects which someone else might destroy.
 * This way, the object won't be destroyed until everyone stops using it.
 * Make sure the number of {@code close/release} calls is 1 more than the number of calls to {@code retain}
 * by the time you are done using the object and it becomes unreachable.
 *
 * <p> This class is thread-safe as long as each thread invokes {@code retain} and {@code close/release}
 * or synchronizes with one that does. The {@code destroy} method, invoked by the last thread which invokes
 * {@code close/release}, will be synchronized (ie, ordered) with all other threads which invoked {@code close/release}.
 *
 * <p> There is no finalization mechanism which tries to call {@code destroy} in case you forget to call release!
 * Finalization presents big challenges, including concurrency issues and even premature finalization,
 * especially in the general case.
 * (If you insist on having finalizers, you may still use them or the higher-performance {@code java.lang.ref.Cleaner}.)
 *
 * <p> Instead, a facility for detecting memory leaks is provided in the form of {@link ResourceLeakDetector}.
 * It uses a similar mechanism to finalization, however because of its narrow scope it works correctly.
 * Leak detection is enabled by default, however you may wish to configure it to trade off overhead
 * and debugging information.
 *
 * @see ResourceLeakDetector
 * @see AutoCloseable
 */
public abstract class ReferenceCountedObject implements AutoCloseable {

    private static final AtomicLongFieldUpdater<ReferenceCountedObject>
        REFERENCE_COUNT_UPDATER = AtomicLongFieldUpdater.newUpdater(ReferenceCountedObject.class, "referenceCount");

    /**
     * Equivalent to calling {@link #ReferenceCountedObject(boolean) this(boolean)} with a value of {@code false}.
     */
    protected ReferenceCountedObject() {
        this(false);
    }

    /**
     * @param idempotentClose if {@code true}, do not throw an exception if {@code close} or {@code release}
     *                        is called more often than necessary, ie if the reference count becomes negative.
     *                        This is the encouraged behavior of {@link AutoCloseable#close()},
     *                        although the contract of that method explicitly states that idempotency is not required.
     *                        Nevertheless, passing {@code false} may help detect bugs.
     *                        Logic which will not {@code release} a destroyed object is more likely to be correct overall,
     *                        particularly if the object may be shared.
     */
    protected ReferenceCountedObject(boolean idempotentClose) {

        this.idempotentClose = idempotentClose;
    }


    private volatile long
        referenceCount = 1;

    private final boolean
        idempotentClose;

    private final ResourceReference
        resourceReference = ResourceLeakDetector.getInstance().tryRegister(this);


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
     * Increments the reference count by {@code 1}.
     *
     * @return this object
     */
    public final ReferenceCountedObject
    retain() {

        long oldCount = REFERENCE_COUNT_UPDATER.getAndIncrement(this);

        if (oldCount <= 0) {
            REFERENCE_COUNT_UPDATER.getAndDecrement(this); // not exactly safe, but better than nothing

            throw new AssertionError("Resurrected a destroyed object"
                + (resourceReference != null ? resourceReference.getTracesString() : ""));
        }

        if (resourceReference != null)
            resourceReference.trace("Object retained. Reference count is now {}", oldCount + 1);

        return this;
    }

    /**
     * Decreases the reference count by {@code 1} and calls {@link #destroy} if the reference count reaches
     * {@code 0}.
     *
     * @return {@code true} if the reference count became {@code 0} and this object was destroyed
     * @implNote Although this would most likely be a logic error anyway,
     * deadlock may occur if another thread has synchronized on this object
     * because this method calls {@code synchronized(this)} to avoid.
     */
    public final boolean
    release() {

        long newCount = REFERENCE_COUNT_UPDATER.decrementAndGet(this);

        if (resourceReference != null)
            resourceReference.trace("Object released. Reference count is now {}.", newCount);

        if (newCount == 0) {
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

            return true;
        } else if (!idempotentClose && newCount <= -1) {
            throw new AssertionError("Tried to release a destroyed object"
                + (resourceReference != null ? resourceReference.getTracesString() : ""));
        } else
            return false;
    }

    /**
     * {@code close} simply calls {@link #release release}.
     * The intent is to allow patterns such as:
     * <pre>
     * try (new MyReferenceCountedObject())
     * { ... }
     *
     * try (refCountedObject.retain())
     * { ... }
     *
     * {@literal @}lombok.Cleanup var a = new MyReferenceCountedObject();
     * </pre>
     */
    public final @Override
    void
    close() {

        release();
    }

    /**
     * Records the stack trace for debugging purposes in case this object is detected to have leaked.
     * You must set the {@link ResourceLeakDetector.Level resource leak detector level}
     * to {@link ResourceLeakDetector.Level#DEBUG DEBUG}.
     *
     * @see ResourceReference#trace(Object)
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
