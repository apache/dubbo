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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeanUtilsTest {

    @Test
    public void testMapToBean() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("s1", "s1");
        map.put("i1", 1);

        List<Map<String, Object>> l1 = new ArrayList<>();
        Map<String, Object> bean2 = new HashMap<>();
        bean2.put("s2", "s2");
        bean2.put("i2", 2);
        l1.add(bean2);
        map.put("l1", l1);

        List<List<Integer>> l2 = new ArrayList<>();
        List<Integer> ll2 = new ArrayList<>();
        ll2.add(2);
        l2.add(ll2);
        map.put("l2", l2);

        Map<String, String> m1 = new HashMap<>();
        m1.put("k", "v");
        map.put("m1", m1);

        Bean1 bean1 = BeanUtils.mapToBean(map, Bean1.class);

        assertEquals(bean1.getS1(), "s1");;
        assertEquals(bean1.getI1(), 1);
        assertEquals(bean1.getL1().size(), 1);
        assertEquals(bean1.getL1().get(0).getS2(), "s2");
        assertEquals(bean1.getL1().get(0).getI2(), 2);
        assertEquals(bean1.getL2().size(), 1);
        assertEquals(bean1.getL2().get(0).size(), 1);
        assertEquals(bean1.getL2().get(0).get(0), 2);
        assertNotNull(bean1.getM1());
        assertTrue(bean1.getM1().containsKey("k"));
        assertEquals(bean1.getM1().get("k"), "v");
    }

    static class Bean1 {
        private String s1;
        private Integer i1;
        private List<Bean2> l1;
        private List<List<Integer>> l2;
        private Map<String, String> m1;

        public String getS1() {
            return s1;
        }

        public void setS1(String s1) {
            this.s1 = s1;
        }

        public Integer getI1() {
            return i1;
        }

        public void setI1(Integer i1) {
            this.i1 = i1;
        }

        public List<Bean2> getL1() {
            return l1;
        }

        public void setL1(List<Bean2> l1) {
            this.l1 = l1;
        }

        public List<List<Integer>> getL2() {
            return l2;
        }

        public void setL2(List<List<Integer>> l2) {
            this.l2 = l2;
        }

        public Map<String, String> getM1() {
            return m1;
        }

        public void setM1(Map<String, String> m1) {
            this.m1 = m1;
        }
    }

    static class Bean2 {
        private String s2;
        private Integer i2;

        public String getS2() {
            return s2;
        }

        public void setS2(String s2) {
            this.s2 = s2;
        }

        public Integer getI2() {
            return i2;
        }

        public void setI2(Integer i2) {
            this.i2 = i2;
        }
    }
}
