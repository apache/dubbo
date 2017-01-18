package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.AvroService;
import org.apache.avro.AvroRemoteException;

/**
 * Created by wuyu on 2016/6/15.
 */
public class AvroServiceImpl implements AvroService {

    @Override
    public CharSequence sayHello(CharSequence id) throws AvroRemoteException {
        return "hello " + id;
    }
}
