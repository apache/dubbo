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
package org.apache.dubbo.xds.resource.endpoint;

public class DropOverload {

    private final String category;

    private final int dropsPerMillion;

    public DropOverload(String category, int dropsPerMillion) {
        if (category == null) {
            throw new NullPointerException("Null category");
        }
        this.category = category;
        this.dropsPerMillion = dropsPerMillion;
    }

    public String getCategory() {
        return category;
    }

    public int getDropsPerMillion() {
        return dropsPerMillion;
    }

    @Override
    public String toString() {
        return "DropOverload{" + "category=" + category + ", " + "dropsPerMillion=" + dropsPerMillion + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof DropOverload) {
            DropOverload that = (DropOverload) o;
            return this.category.equals(that.getCategory()) && this.dropsPerMillion == that.getDropsPerMillion();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= category.hashCode();
        h$ *= 1000003;
        h$ ^= dropsPerMillion;
        return h$;
    }
}
