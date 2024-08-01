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
package org.apache.dubbo.xds.resource_new.common;

public final class FractionalPercent {

    enum DenominatorType {
        HUNDRED,
        TEN_THOUSAND,
        MILLION
    }

    private final int numerator;

    private final DenominatorType denominatorType;

    public static FractionalPercent perHundred(int numerator) {
        return FractionalPercent.create(numerator, FractionalPercent.DenominatorType.HUNDRED);
    }

    public static FractionalPercent perTenThousand(int numerator) {
        return FractionalPercent.create(numerator, FractionalPercent.DenominatorType.TEN_THOUSAND);
    }

    public static FractionalPercent perMillion(int numerator) {
        return FractionalPercent.create(numerator, FractionalPercent.DenominatorType.MILLION);
    }

    public static FractionalPercent create(int numerator, FractionalPercent.DenominatorType denominatorType) {
        return new FractionalPercent(numerator, denominatorType);
    }

    public FractionalPercent(int numerator, DenominatorType denominatorType) {
        this.numerator = numerator;
        if (denominatorType == null) {
            throw new NullPointerException("Null denominatorType");
        }
        this.denominatorType = denominatorType;
    }

    int numerator() {
        return numerator;
    }

    DenominatorType denominatorType() {
        return denominatorType;
    }

    @Override
    public String toString() {
        return "FractionalPercent{" + "numerator=" + numerator + ", " + "denominatorType=" + denominatorType + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof FractionalPercent) {
            FractionalPercent that = (FractionalPercent) o;
            return this.numerator == that.numerator() && this.denominatorType.equals(that.denominatorType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= numerator;
        h$ *= 1000003;
        h$ ^= denominatorType.hashCode();
        return h$;
    }
}
