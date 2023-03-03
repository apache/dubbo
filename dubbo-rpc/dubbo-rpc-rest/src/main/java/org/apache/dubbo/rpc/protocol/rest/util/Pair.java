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
package org.apache.dubbo.rpc.protocol.rest.util;

public class Pair<X, Y> {
    private final X x;
    private final Y y;

    public Pair(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public X getFirst() {
        return x;
    }

    public Y getSecond() {
        return y;
    }

    public static <A, B> Pair<A, B> make(A a, B b) {
        return new Pair<A, B>(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Pair))
            return false;

        Pair other = (Pair) o;

        if (x == null) {
            if (other.x != null)
                return false;
        } else {
            if (!x.equals(other.x))
                return false;
        }
        if (y == null) {
            if (other.y != null)
                return false;
        } else {
            if (!y.equals(other.y))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        if (x != null)
            hashCode = x.hashCode();
        if (y != null)
            hashCode = (hashCode * 31) + y.hashCode();
        return hashCode;
    }

    @Override
    public String toString() {
        return "P[" + x + "," + y + "]";
    }


}
