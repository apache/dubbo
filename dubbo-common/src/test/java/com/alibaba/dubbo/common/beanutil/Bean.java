package com.alibaba.dubbo.common.beanutil;

import com.alibaba.dubbo.common.model.person.FullAddress;
import com.alibaba.dubbo.common.model.person.PersonStatus;
import com.alibaba.dubbo.common.model.person.Phone;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
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
