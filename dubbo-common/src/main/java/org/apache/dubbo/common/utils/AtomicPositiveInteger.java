/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.utils;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AtomicPositiveInteger extends Number {

    private static final long serialVersionUID = -3038533876489105940L;

    private static final AtomicIntegerFieldUpdater<AtomicPositiveInteger> INDEX_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AtomicPositiveInteger.class, "index");

    @SuppressWarnings("unused")
    private volatile int index = 0;

    public AtomicPositiveInteger() {
    }

    public AtomicPositiveInteger(int initialValue) {
        INDEX_UPDATER.set(this, initialValue);
    }

    public final int getAndIncrement() {
        return INDEX_UPDATER.getAndIncrement(this) & Integer.MAX_VALUE;
    }

    public final int getAndDecrement() {
        return INDEX_UPDATER.getAndDecrement(this) & Integer.MAX_VALUE;
    }

    public final int incrementAndGet() {
        return INDEX_UPDATER.incrementAndGet(this) & Integer.MAX_VALUE;
    }

    public final int decrementAndGet() {
        return INDEX_UPDATER.decrementAndGet(this) & Integer.MAX_VALUE;
    }

    public final int get() {
        return INDEX_UPDATER.get(this) & Integer.MAX_VALUE;
    }

    public final void set(int newValue) {
        if (newValue < 0) {
            throw new IllegalArgumentException("new value " + newValue + " < 0");
        }
        INDEX_UPDATER.set(this, newValue);
    }

    public final int getAndSet(int newValue) {
        if (newValue < 0) {
            throw new IllegalArgumentException("new value " + newValue + " < 0");
        }
        return INDEX_UPDATER.getAndSet(this, newValue) & Integer.MAX_VALUE;
    }

    public final int getAndAdd(int delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("delta " + delta + " < 0");
        }
        return INDEX_UPDATER.getAndAdd(this, delta) & Integer.MAX_VALUE;
    }

    public final int addAndGet(int delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("delta " + delta + " < 0");
        }
        return INDEX_UPDATER.addAndGet(this, delta) & Integer.MAX_VALUE;
    }

    public final boolean compareAndSet(int expect, int update) {
        if (update < 0) {
            throw new IllegalArgumentException("update value " + update + " < 0");
        }
        return INDEX_UPDATER.compareAndSet(this, expect, update);
    }

    public final boolean weakCompareAndSet(int expect, int update) {
        if (update < 0) {
            throw new IllegalArgumentException("update value " + update + " < 0");
        }
        return INDEX_UPDATER.weakCompareAndSet(this, expect, update);
    }

    @Override
    public byte byteValue() {
        return (byte) get();
    }

    @Override
    public short shortValue() {
        return (short) get();
    }

    @Override
    public int intValue() {
        return get();
    }

    @Override
    public long longValue() {
        return (long) get();
    }

    @Override
    public float floatValue() {
        return (float) get();
    }

    @Override
    public double doubleValue() {
        return (double) get();
    }

    @Override
    public String toString() {
        return Integer.toString(get());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + get();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AtomicPositiveInteger)) {
            return false;
        }
        AtomicPositiveInteger other = (AtomicPositiveInteger) obj;
        return intValue() == other.intValue();
    }
}