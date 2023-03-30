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
package org.apache.dubbo.common.model.person;

import java.io.Serializable;

public class Cgeneric<T> implements Serializable {
    public static String NAME = "C";

    private String name = NAME;
    private T data;
    private Ageneric<T> a;
    private Bgeneric<PersonInfo> b;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public Ageneric<T> getA() {
        return a;
    }

    public void setA(Ageneric<T> a) {
        this.a = a;
    }

    public Bgeneric<PersonInfo> getB() {
        return b;
    }

    public void setB(Bgeneric<PersonInfo> b) {
        this.b = b;
    }
}
