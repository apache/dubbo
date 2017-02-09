package com.alibaba.dubbo.rpc.protocol.redis2;

import com.alibaba.dubbo.common.utils.PojoUtils;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import redis.netty4.*;
import redis.server.netty.RedisCommandHandler;
import redis.server.netty.RedisException;
import redis.util.BytesKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/2/7.
 */
@ChannelHandler.Sharable
public class RpcRedisCommandHandler extends RedisCommandHandler {

    private Map<BytesKey, Wrapper> beanMethods = new ConcurrentHashMap<>();

    private MappingJackson2CommandConverter converter = new MappingJackson2CommandConverter();

    private RpcSimpleRedisServer rpcSimpleRedisServer;

    public RpcRedisCommandHandler(RpcSimpleRedisServer rs) {
        super(rs);
        this.rpcSimpleRedisServer = rs;
    }

    interface Wrapper {
        Reply execute(Command command) throws RedisException;
    }

    public <T> void removeHandler(Class<T> type) {
        for (Method method : type.getMethods()) {
            String name = method.getName();
            this.beanMethods.remove(new BytesKey((type.getName() + "." + name).toLowerCase().getBytes()));
        }
    }

    public <T> void addHandler(final Class<T> type, final T impl) {
        addHandler(type.getName(), type, impl);
    }

    public <T> void addHandler(String key, final Class<T> type, final T impl) {
        rpcSimpleRedisServer.addServiceKey(key);
        for (final Method method : type.getMethods()) {
            final Type[] types = method.getGenericParameterTypes();
            String name = type.getName() + "." + method.getName();
            beanMethods.put(new BytesKey(name.toLowerCase().getBytes()), new Wrapper() {
                @Override
                public Reply execute(Command command) throws RedisException {
                    Object[] objects = command.getObjects();
                    Object[] params = new Object[types.length];
                    try {
                        ObjectMapper objectMapper = converter.getObjectMapper();
                        for (int i = 0; i < params.length; i++) {
                            JavaType javaType = converter.getJavaType(types[i]);
                            params[i] = objectMapper.convertValue(new String((byte[]) objects[i + 1], "utf-8"), javaType);
                        }

                        Object result = method.invoke(impl, params);
                        if (result == null) {
                            return new InlineReply(null);
                        }

                        if (result instanceof String) {
                            return new BulkReply(result.toString().getBytes("utf-8"));
                        }

                        if (result instanceof byte[]) {
                            return new BulkReply((byte[]) result);
                        }

                        return new BulkReply(objectMapper.writeValueAsBytes(result));
                    } catch (IllegalAccessException e) {
                        throw new RedisException("Invalid server implementation");
                    } catch (InvocationTargetException e) {
                        Throwable te = e.getTargetException();
                        if (!(te instanceof RedisException)) {
                            te.printStackTrace();
                        }
                        return new ErrorReply("ERR " + te.getMessage());
                    } catch (Exception e) {
                        return new ErrorReply("ERR " + e.getMessage());
                    }
                }
            });
        }
    }

    private static final byte LOWER_DIFF = 'a' - 'A';

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        byte[] name = msg.getName();
        for (int i = 0; i < name.length; i++) {
            byte b = name[i];
            if (b >= 'A' && b <= 'Z') {
                name[i] = (byte) (b + LOWER_DIFF);
            }
        }
        Wrapper wrapper = beanMethods.get(new BytesKey(name));
        if (wrapper != null) {
            Reply reply = wrapper.execute(msg);
            ctx.write(reply);
        } else {
            super.channelRead0(ctx, msg);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }


}
