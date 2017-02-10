package com.alibaba.dubbo.rpc.protocol.redis2;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Connection;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Created by wuyu on 2017/2/9.
 */
public class Redis2Client {

    private GenericObjectPool<Redis2Connection> genericObjectPool;

    private ObjectMapper objectMapper = new ObjectMapper();

    public Redis2Client(Redis2ConnectionPoolFactory redis2ConnectionPoolFactory, GenericObjectPoolConfig config) {
        this.genericObjectPool = new GenericObjectPool<>(redis2ConnectionPoolFactory, config);
    }

    public Object invoke(Class type, Method method, Object[] args) throws Throwable {
        Redis2Connection redis2Connection = genericObjectPool.borrowObject();
        try {
            String key = type.getName() + "." + method.getName();
            Type genericReturnType = method.getGenericReturnType();

            String reply = null;
            if (args != null && args.length > 0) {
                String[] params = new String[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof String) {
                        params[i] = (String) args[i];
                    } else if (args[i] == null) {
                        params[i] = "(nil)";
                    } else {
                        params[i] = objectMapper.writeValueAsString(args[i]);
                    }
                }
                Connection connection = redis2Connection.sendCommand(key, params);
                reply = connection.getBulkReply();
            } else {
                Connection connection = redis2Connection.sendCommand(key, "(nil)");
                reply = connection.getBulkReply();
            }

            JavaType javaType = TypeFactory.defaultInstance().constructType(genericReturnType);

            if (void.class.isAssignableFrom(method.getReturnType())) {
                return null;
            } else if (String.class.isAssignableFrom(method.getReturnType())) {
                return reply;
            } else if (objectMapper.canDeserialize(javaType)) {
                return objectMapper.readValue(reply, javaType);
            }
            return objectMapper.convertValue(reply, javaType);
        } finally {
            genericObjectPool.returnObject(redis2Connection);
        }
    }

    public void close() {
        genericObjectPool.close();
    }
}
