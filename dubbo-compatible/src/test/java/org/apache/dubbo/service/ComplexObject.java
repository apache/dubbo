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
package org.apache.dubbo.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ON 2018/11/5
 */
public class ComplexObject {

    public ComplexObject() {
    }

    public ComplexObject(String var1, int var2, long l, String[] var3, List<Integer> var4, ComplexObject.TestEnum testEnum) {
        this.setInnerObject(new ComplexObject.InnerObject());
        this.getInnerObject().setInnerA(var1);
        this.getInnerObject().setInnerB(var2);
        this.setIntList(var4);
        this.setStrArrays(var3);
        this.setTestEnum(testEnum);
        this.setV(l);
        InnerObject2 io21 = new InnerObject2();
        io21.setInnerA2(var1 + "_21");
        io21.setInnerB2(var2 + 100000);
        InnerObject2 io22 = new InnerObject2();
        io22.setInnerA2(var1 + "_22");
        io22.setInnerB2(var2 + 200000);
        this.setInnerObject2(new ArrayList<>(Arrays.asList(io21, io22)));

        InnerObject3 io31 = new InnerObject3();
        io31.setInnerA3(var1 + "_31");
        InnerObject3 io32 = new InnerObject3();
        io32.setInnerA3(var1 + "_32");
        InnerObject3 io33 = new InnerObject3();
        io33.setInnerA3(var1 + "_33");
        this.setInnerObject3(new InnerObject3[]{io31, io32, io33});
        this.maps = new HashMap<>(4);
        this.maps.put(var1 + "_k1", var1 + "_v1");
        this.maps.put(var1 + "_k2", var1 + "_v2");
    }

    private InnerObject innerObject;
    private List<InnerObject2> innerObject2;
    private InnerObject3[] innerObject3;
    private String[] strArrays;
    private List<Integer> intList;
    private long v;
    private TestEnum testEnum;
    private Map<String, String> maps;

    public InnerObject getInnerObject() {
        return innerObject;
    }

    public void setInnerObject(InnerObject innerObject) {
        this.innerObject = innerObject;
    }

    public String[] getStrArrays() {
        return strArrays;
    }

    public void setStrArrays(String[] strArrays) {
        this.strArrays = strArrays;
    }

    public List<Integer> getIntList() {
        return intList;
    }

    public void setIntList(List<Integer> intList) {
        this.intList = intList;
    }

    public long getV() {
        return v;
    }

    public void setV(long v) {
        this.v = v;
    }

    public TestEnum getTestEnum() {
        return testEnum;
    }

    public void setTestEnum(TestEnum testEnum) {
        this.testEnum = testEnum;
    }

    public List<InnerObject2> getInnerObject2() {
        return innerObject2;
    }

    public void setInnerObject2(List<InnerObject2> innerObject2) {
        this.innerObject2 = innerObject2;
    }

    public InnerObject3[] getInnerObject3() {
        return innerObject3;
    }

    public void setInnerObject3(InnerObject3[] innerObject3) {
        this.innerObject3 = innerObject3;
    }

    public Map<String, String> getMaps() {
        return maps;
    }

    public void setMaps(Map<String, String> maps) {
        this.maps = maps;
    }

    @Override
    public String toString() {
        return "ComplexObject{" +
                "innerObject=" + innerObject +
                ", innerObject2=" + innerObject2 +
                ", innerObject3=" + Arrays.toString(innerObject3) +
                ", strArrays=" + Arrays.toString(strArrays) +
                ", intList=" + intList +
                ", v=" + v +
                ", testEnum=" + testEnum +
                ", maps=" + maps +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComplexObject)) return false;
        ComplexObject that = (ComplexObject) o;
        return getV() == that.getV() &&
                Objects.equals(getInnerObject(), that.getInnerObject()) &&
                Objects.equals(getInnerObject2(), that.getInnerObject2()) &&
                Arrays.equals(getInnerObject3(), that.getInnerObject3()) &&
                Arrays.equals(getStrArrays(), that.getStrArrays()) &&
                Objects.equals(getIntList(), that.getIntList()) &&
                getTestEnum() == that.getTestEnum() &&
                Objects.equals(getMaps(), that.getMaps());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getInnerObject(), getInnerObject2(), getIntList(), getV(), getTestEnum(), getMaps());
        result = 31 * result + Arrays.hashCode(getInnerObject3());
        result = 31 * result + Arrays.hashCode(getStrArrays());
        return result;
    }

    public enum TestEnum {
        VALUE1, VALUE2
    }

    static public class InnerObject {
        String innerA;
        int innerB;

        public String getInnerA() {
            return innerA;
        }

        public void setInnerA(String innerA) {
            this.innerA = innerA;
        }

        public int getInnerB() {
            return innerB;
        }

        public void setInnerB(int innerB) {
            this.innerB = innerB;
        }

        @Override
        public String toString() {
            return "InnerObject{" +
                    "innerA='" + innerA + '\'' +
                    ", innerB=" + innerB +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InnerObject)) return false;
            InnerObject that = (InnerObject) o;
            return getInnerB() == that.getInnerB() &&
                    Objects.equals(getInnerA(), that.getInnerA());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getInnerA(), getInnerB());
        }
    }

    static public class InnerObject2 {
        String innerA2;
        int innerB2;

        public String getInnerA2() {
            return innerA2;
        }

        public void setInnerA2(String innerA2) {
            this.innerA2 = innerA2;
        }

        public int getInnerB2() {
            return innerB2;
        }

        public void setInnerB2(int innerB2) {
            this.innerB2 = innerB2;
        }

        @Override
        public String toString() {
            return "InnerObject{" +
                    "innerA='" + innerA2 + '\'' +
                    ", innerB=" + innerB2 +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InnerObject2)) return false;
            InnerObject2 that = (InnerObject2) o;
            return getInnerB2() == that.getInnerB2() &&
                    Objects.equals(getInnerA2(), that.getInnerA2());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getInnerA2(), getInnerB2());
        }
    }

    static public class InnerObject3 {
        String innerA3;

        public String getInnerA3() {
            return innerA3;
        }

        public void setInnerA3(String innerA3) {
            this.innerA3 = innerA3;
        }

        @Override
        public String toString() {
            return "InnerObject3{" +
                    "innerA3='" + innerA3 + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InnerObject3)) return false;
            InnerObject3 that = (InnerObject3) o;
            return Objects.equals(getInnerA3(), that.getInnerA3());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getInnerA3());
        }
    }
}


