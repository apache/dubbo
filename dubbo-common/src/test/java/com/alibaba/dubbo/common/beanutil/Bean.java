package com.alibaba.dubbo.common.beanutil;

import com.alibaba.dubbo.common.model.person.FullAddress;
import com.alibaba.dubbo.common.model.person.PersonStatus;
import com.alibaba.dubbo.common.model.person.Phone;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
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
public class Bean {

    private Class<?> type;

    private PersonStatus status;

    private Date date;

    private Phone[] array;

    private Collection<Phone> collection;

    private Map<String, FullAddress> addresses;

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public PersonStatus getStatus() {
        return status;
    }

    public void setStatus(PersonStatus status) {
        this.status = status;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Phone[] getArray() {
        return array;
    }

    public void setArray(Phone[] array) {
        this.array = array;
    }

    public Collection<Phone> getCollection() {
        return collection;
    }

    public void setCollection(Collection<Phone> collection) {
        this.collection = collection;
    }

    public Map<String, FullAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(Map<String, FullAddress> addresses) {
        this.addresses = addresses;
    }
}
