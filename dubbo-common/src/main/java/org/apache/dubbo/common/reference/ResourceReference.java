/*
 * Copyright 2013 The Netty Project
 * Copyright 2018 Aleksandr Dubinsky
 *
 * The Netty Project licenses this file to you under the Apache License,
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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Used by ResourceLeakDetector to track leaks.
 *
 * @author Aleksandr Dubinsky
 */
final class ResourceReference extends WeakReference<Object> implements Iterable<ResourceReference> {

    private static final AtomicReferenceFieldUpdater<ResourceReference, Trace>
        TRACELISTHEAD_UPDATER = AtomicReferenceFieldUpdater.newUpdater(ResourceReference.class, Trace.class, "traceListHead");

    private static final AtomicIntegerFieldUpdater<ResourceReference>
        NDROPPEDTRACES_UPDATER = AtomicIntegerFieldUpdater.newUpdater(ResourceReference.class, "nDroppedTraces");

    // Doubly-linked list of all ResourceReferences.
    // This is necessary because if a Weak/PhantomReference is not kept reachable, it will never be put on a ReferenceQueue.
    // This implementation idea was taken from java.lang.ref.Cleaner (in JDK 9)
    private final ResourceReference refListHead;
    private ResourceReference next = this, prev = this;

    private volatile Trace traceListHead;
    private volatile int nDroppedTraces;

    private final String referentClassName;
    private final int targetTraceCount;

    private final Set<String> suppressedStackTraceEntries;

    private static final String NEWLINE = System.lineSeparator();

    ResourceReference
        (Object referent
            , ReferenceQueue<Object> refQueue
            , ResourceReference refListHead
            , int traceCount
            , Set<String> suppressedStackTraceEntries) {

        super(referent, refQueue);

        this.refListHead = refListHead;
        TRACELISTHEAD_UPDATER.set(this, traceCount == 0 ? Trace.BOTTOM : new Trace(Trace.BOTTOM, null));
        this.nDroppedTraces = 0;
        this.referentClassName = referent.getClass().getName();
        this.targetTraceCount = traceCount;
        this.suppressedStackTraceEntries = suppressedStackTraceEntries;

        synchronized (referent) {
            insert();
        }

        // jdk.internal.ref.PhantomCleanable has the following code at the end of the method,
        // suggesting that the referent might already be unreachable?! What?!
        // I guess I have to insert a synchronization block around `insert`.
        //        // Ensure referent and cleaner remain accessible
        //        Reference.reachabilityFence(referent);
        //        Reference.reachabilityFence(cleaner);
    }

    /**
     * Construct a new root of the list; not inserted.
     */
    ResourceReference() {

        super(null, null);
        this.refListHead = this;
        this.referentClassName = null;
        this.targetTraceCount = 0;
        this.suppressedStackTraceEntries = null;
    }

    /**
     * Insert this ResourceReference after the list head.
     */
    private void
    insert() {
        synchronized (refListHead) {
            prev = refListHead;
            next = refListHead.next;
            next.prev = this;
            refListHead.next = this;
        }
    }

    /**
     * Remove this ResourceReference from the list.
     *
     * @return true if Cleanable was removed or false if not because
     * it had already been removed before
     */
    private boolean
    remove() {
        synchronized (refListHead) {
            if (next != this) {
                next.prev = prev;
                prev.next = next;
                prev = this;
                next = this;
                return true;
            }
            return false;
        }
    }

    public @Override
    Iterator<ResourceReference>
    iterator() {

        assert Thread.holdsLock(refListHead);

        return new Iterator<ResourceReference>() {
            ResourceReference cur = refListHead;

            public @Override
            boolean
            hasNext() {

                return cur.next != cur;
            }

            public @Override
            ResourceReference
            next() {

                cur = cur.next;
                return cur;
            }
        };
    }

    /**
     * Unregisters the resource from the leak detector.
     */
    public void
    unregister() {

        remove();
        super.clear(); // try to prevent the reference from being enqueued
    }

    public void
    trace(String message, Object param1) {

        if (targetTraceCount <= 1)
            return;

        trace(MessageFormatter.format(message, param1).getMessage());
    }

    public void
    trace(String message, Object param1, Object param2) {

        if (targetTraceCount <= 1)
            return;

        trace(MessageFormatter.format(message, param1, param2).getMessage());
    }

    public void
    trace(String message, Object... argArray) {

        if (targetTraceCount <= 1)
            return;

        trace(MessageFormatter.format(message, argArray).getMessage());
    }

    /**
     * This method works by exponentially backing off as more records are present in the stack. Each record has a
     * 1 / 2^n chance of dropping the top most record and replacing it with itself. This has a number of convenient
     * properties:
     *
     * <ol>
     * <li>  The current record is always recorded. This is due to the compare and swap dropping the top most
     *       record, rather than the to-be-pushed record.
     * <li>  The very last access will always be recorded. This comes as a property of 1.
     * <li>  It is possible to retain more records than the target, based upon the probability distribution.
     * <li>  It is easy to keep a precise record of the number of elements in the stack, since each element has to
     *     know how tall the stack is.
     * </ol>
     * <p>
     * In this particular implementation, there are also some advantages. A thread local random is used to decide
     * if something should be recorded. This means that if there is a deterministic access pattern, it is now
     * possible to see what other accesses occur, rather than always dropping them. Second, after
     * {@link #TARGET_RECORDS} accesses, backoff occurs. This matches typical access patterns,
     * where there are either a high number of accesses (i.e. a cached buffer), or low (an ephemeral buffer), but
     * not many in between.
     * <p>
     * The use of atomics avoids serializing a high number of accesses, when most of the records will be thrown
     * away. High contention only happens when there are very few existing records, which is only likely when the
     * object isn't shared! If this is a problem, the loop can be aborted and the record dropped, because another
     * thread won the race.
     */
    public void
    trace(Object message) {

        if (targetTraceCount <= 1)
            return;

        Trace oldHead;
        Trace newHead;
        boolean dropped;
        do {
            oldHead = traceListHead;

            final int numElements = oldHead.pos + 1;
            if (numElements >= targetTraceCount) {
                final int backOffFactor = Math.min(numElements - targetTraceCount, 30);
                dropped = ThreadLocalRandom.current().nextInt(1 << backOffFactor) != 0;
            } else
                dropped = false;

            if (!dropped)
                newHead = new Trace(oldHead, message);
            else
                // We will replace oldHead, guaranteeing we record the last access
                // This is very slow, so shouldn't we only do it when level == PARANOID?
                newHead = new Trace(oldHead.prev, message);
        }
        while (!TRACELISTHEAD_UPDATER.compareAndSet(this, oldHead, newHead));

        if (dropped)
            NDROPPEDTRACES_UPDATER.incrementAndGet(this);
    }

    public String
    getReferentClassName() {
        return referentClassName;
    }

    public String
    getTracesString() {

        Trace head = traceListHead;

        int nTraces = head.pos + 1;
        int nDropped = nDroppedTraces;
        int nDuplicates = 0;

        if (nTraces == 0)
            return "";

        // Guess about 2 kilobytes per stack trace
        StringBuilder buf = new StringBuilder(nTraces * 2048).append(NEWLINE);

        Set<String> seen = new HashSet<>(nTraces);
        for (int i = 1; head != Trace.BOTTOM; i++, head = head.prev) {
            String stackTrace = getStackTraceString(head);
            if (seen.add(stackTrace)) {
                if (head.prev == Trace.BOTTOM)
                    buf.append("\tObject was allocated:").append(NEWLINE)
                        .append(stackTrace);
                else if (i == 1 && nTraces > 1)
                    buf.append("\tLast call to trace(): ").append(head.message != null ? head.message : "")
                        .append(NEWLINE)
                        .append(stackTrace);
                else
                    buf.append("\tRecent call to trace() #").append(i).append(": ").append(head.message != null ? head.message : "")
                        .append(NEWLINE)
                        .append(stackTrace);
            } else
                nDuplicates++;
        }

        if (nDuplicates > 0) {
            buf.append("\t")
                .append(nDuplicates)
                .append(" traces were discarded because they were duplicates")
                .append(NEWLINE);
        }

        if (nDropped > 0) {
            buf.append("\t")
                .append(nDropped)
                .append(" traces were discarded because the target trace count is ")
                .append(targetTraceCount)
                .append(". Use system property ")
                .append(ResourceLeakDetector.PROP_TRACE_COUNT)
                .append(" to increase the limit.")
                .append(NEWLINE);
        }

        buf.setLength(buf.length() - NEWLINE.length());
        return buf.toString();
    }

    private String
    getStackTraceString(Throwable t) {

        StringBuilder buf = new StringBuilder(2048);

        StackTraceElement[] array = t.getStackTrace();
        for (int i = 0; i < array.length; i++) {
            StackTraceElement element = array[i];

            if (suppressedStackTraceEntries.contains(element.getClassName() + "." + element.getMethodName()))
                continue;

            buf.append("\t\tat ");
            buf.append(element.toString());
            buf.append(NEWLINE);
        }
        return buf.toString();
    }

    private static final
    class Trace extends Throwable {
        private static final long serialVersionUID = 6065153674892850720L;

        static final Trace BOTTOM = new Trace();

        final String message;
        final Trace prev;
        final int pos;

        Trace(Trace prev, Object message) {

            this.message = message == null ? null : message.toString();
            this.prev = prev;
            this.pos = prev.pos + 1;
        }

        // Used to terminate the linked list
        private Trace() {

            message = null;
            prev = null;
            pos = -1;
        }
    }
}
