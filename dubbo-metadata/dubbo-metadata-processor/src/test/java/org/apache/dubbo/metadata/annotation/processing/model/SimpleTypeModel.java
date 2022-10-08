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
package org.apache.dubbo.metadata.annotation.processing.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * Simple Type model
 *
 * @since 2.7.6
 */
public class SimpleTypeModel {

    private Void v;

    private Boolean z;

    private Character c;

    private Byte b;

    private Short s;

    private Integer i;

    private Long l;

    private Float f;

    private Double d;

    private String str;

    private BigDecimal bd;

    private BigInteger bi;

    private Date dt;

    private int invalid;

    public Void getV() {
        return v;
    }

    public void setV(Void v) {
        this.v = v;
    }

    public Boolean getZ() {
        return z;
    }

    public void setZ(Boolean z) {
        this.z = z;
    }

    public Character getC() {
        return c;
    }

    public void setC(Character c) {
        this.c = c;
    }

    public Byte getB() {
        return b;
    }

    public void setB(Byte b) {
        this.b = b;
    }

    public Short getS() {
        return s;
    }

    public void setS(Short s) {
        this.s = s;
    }

    public Integer getI() {
        return i;
    }

    public void setI(Integer i) {
        this.i = i;
    }

    public Long getL() {
        return l;
    }

    public void setL(Long l) {
        this.l = l;
    }

    public Float getF() {
        return f;
    }

    public void setF(Float f) {
        this.f = f;
    }

    public Double getD() {
        return d;
    }

    public void setD(Double d) {
        this.d = d;
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public BigDecimal getBd() {
        return bd;
    }

    public void setBd(BigDecimal bd) {
        this.bd = bd;
    }

    public BigInteger getBi() {
        return bi;
    }

    public void setBi(BigInteger bi) {
        this.bi = bi;
    }

    public Date getDt() {
        return dt;
    }

    public void setDt(Date dt) {
        this.dt = dt;
    }
}
