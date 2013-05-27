package com.alibaba.dubbo.config.spring;

import com.alibaba.dubbo.rpc.service.GenericException;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class GenericDemoService implements GenericService {

    public Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException {
        return null;
    }
}
