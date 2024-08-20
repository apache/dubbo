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
package org.apache.dubbo.xds.resource.matcher;

public final class FractionMatcher {

    private final int numerator;

    private final int denominator;

    public static FractionMatcher create(int numerator, int denominator) {
        return new FractionMatcher(numerator, denominator);
    }

    FractionMatcher(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public int getNumerator() {
        return numerator;
    }

    public int getDenominator() {
        return denominator;
    }

    @Override
    public String toString() {
        return "FractionMatcher{" + "numerator=" + numerator + ", " + "denominator=" + denominator + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof FractionMatcher) {
            FractionMatcher that = (FractionMatcher) o;
            return this.numerator == that.getNumerator() && this.denominator == that.getDenominator();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= numerator;
        h$ *= 1000003;
        h$ ^= denominator;
        return h$;
    }
}
