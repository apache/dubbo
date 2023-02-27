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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match;


public class DubboMethodArg {
    private int index;
    private String type;
    private ListStringMatch str_value;
    private ListDoubleMatch num_value;
    private BoolMatch bool_value;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ListStringMatch getStr_value() {
        return str_value;
    }

    public void setStr_value(ListStringMatch str_value) {
        this.str_value = str_value;
    }

    public ListDoubleMatch getNum_value() {
        return num_value;
    }

    public void setNum_value(ListDoubleMatch num_value) {
        this.num_value = num_value;
    }

    public BoolMatch getBool_value() {
        return bool_value;
    }

    public void setBool_value(BoolMatch bool_value) {
        this.bool_value = bool_value;
    }

    public boolean isMatch(Object input) {

        if (str_value != null) {
            return input instanceof String && str_value.isMatch((String) input);
        } else if (num_value != null) {
            return num_value.isMatch(Double.valueOf(input.toString()));
        } else if (bool_value != null) {
            return input instanceof Boolean && bool_value.isMatch((Boolean) input);
        }
        return false;
    }

    @Override
    public String toString() {
        return "DubboMethodArg{" +
                "index=" + index +
                ", type='" + type + '\'' +
                ", str_value=" + str_value +
                ", num_value=" + num_value +
                ", bool_value=" + bool_value +
                '}';
    }
}
