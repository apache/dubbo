package org.apache.dubbo.common.serialize.fastjson2;

import org.apache.dubbo.common.utils.AllowClassNotifyListener;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.filter.Filter;

import java.util.Set;

public class Fastjson2SecurityManager {
    private Filter securityFilter = JSONReader.autoTypeFilter();

    public void notify(Set<String> prefixList) {
        this.securityFilter = JSONReader.autoTypeFilter(prefixList.toArray(new String[0]));
    }

    public Filter getSecurityFilter() {
        return securityFilter;
    }
}
