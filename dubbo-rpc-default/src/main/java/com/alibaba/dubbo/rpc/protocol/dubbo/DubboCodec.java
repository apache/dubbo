/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.dubbo;

import static com.alibaba.dubbo.rpc.protocol.dubbo.CallbackServiceCodec.decodeInvocationArgument;
import static com.alibaba.dubbo.rpc.protocol.dubbo.CallbackServiceCodec.encodeInvocationArgument;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec;
import com.alibaba.dubbo.remoting.exchange.codec.ExchangeCodec;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.support.RpcUtils;

/**
 * Dubbo codec.
 * 
 * @author qianlei
 * @author chao.liuc
 */
public class DubboCodec extends ExchangeCodec implements Codec {

    public static final String      NAME                    = "dubbo";

    private static final String     DUBBO_VERSION           = Version.getVersion(DubboCodec.class, Version.getVersion());

    private static final byte       RESPONSE_WITH_EXCEPTION = 0;

    private static final byte       RESPONSE_VALUE          = 1;

    private static final byte       RESPONSE_NULL_VALUE     = 2;

    private static final Object[]   EMPTY_OBJECT_ARRAY      = new Object[0];

    private static final Class<?>[] EMPTY_CLASS_ARRAY       = new Class<?>[0];
    
   
    @Override
    protected void encodeRequestData(Channel channel, ObjectOutput out, Object data) throws IOException {
        RpcInvocation inv = (RpcInvocation) data;

        out.writeUTF(inv.getAttachment(Constants.DUBBO_VERSION_KEY, DUBBO_VERSION));
        out.writeUTF(inv.getAttachment(Constants.PATH_KEY));
        out.writeUTF(inv.getAttachment(Constants.VERSION_KEY));

        out.writeUTF(inv.getMethodName());
        out.writeUTF(ReflectUtils.getDesc(inv.getParameterTypes()));
        Object[] args = inv.getArguments();
        if (args != null)
        for (int i = 0; i < args.length; i++){
            out.writeObject(encodeInvocationArgument(channel, inv, i));
        }
        out.writeObject(inv.getAttachments());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object decodeRequestData(Channel channel, ObjectInput in) throws IOException {
        RpcInvocation inv = new RpcInvocation();

        inv.setAttachment(Constants.DUBBO_VERSION_KEY, in.readUTF());
        inv.setAttachment(Constants.PATH_KEY, in.readUTF());
        inv.setAttachment(Constants.VERSION_KEY, in.readUTF());

        inv.setMethodName(in.readUTF());
        try {
            Object[] args;
            Class<?>[] pts;
            String desc = in.readUTF();
            if (desc.length() == 0) {
                pts = EMPTY_CLASS_ARRAY;
                args = EMPTY_OBJECT_ARRAY;
            } else {
                pts = ReflectUtils.desc2classArray(desc);
                args = new Object[pts.length];
                for (int i = 0; i < args.length; i++){
                    try{
                        args[i] = in.readObject(pts[i]);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            inv.setParameterTypes(pts);
            
            Map<String, String> map = (Map<String, String>) in.readObject(Map.class);
            if (map != null && map.size() > 0) {
                Map<String, String> attachment = inv.getAttachments();
                if (attachment == null) {
                    attachment = new HashMap<String, String>();
                }
                attachment.putAll(map);
                inv.setAttachments(attachment);
            }
            //decode argument ,may be callback
            for (int i = 0; i < args.length; i++){
                args[i] = decodeInvocationArgument(channel, inv, pts, i, args[i]);
            }
            
            inv.setArguments(args);
            
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read invocation data failed.", e));
        }
        return inv;
    }

    @Override
    protected void encodeResponseData(Channel channel, ObjectOutput out, Object data) throws IOException {
        Result result = (Result) data;

        Throwable th = result.getException();
        if (th == null) {
            Object ret = result.getResult();
            if (ret == null) {
                out.writeByte(RESPONSE_NULL_VALUE);
            } else {
                out.writeByte(RESPONSE_VALUE);
                out.writeObject(ret);
            }
        } else {
            out.writeByte(RESPONSE_WITH_EXCEPTION);
            out.writeObject(th);
        }
    }
    
    @Override
    protected Object decodeResponseData(Channel channel, ObjectInput in, Object request) throws IOException {
        Invocation invocation = (Invocation) request;
        RpcResult result = new RpcResult();

        byte flag = in.readByte();
        switch (flag) {
            case RESPONSE_NULL_VALUE:
                break;
            case RESPONSE_VALUE:
                try {
                    Type[] returnType = RpcUtils.getReturnTypes(invocation);
                    result.setResult(returnType == null || returnType.length == 0 ? in.readObject() : 
                        (returnType.length == 1 ? in.readObject((Class<?>)returnType[0]) 
                                : in.readObject((Class<?>)returnType[0], returnType[1])));
                } catch (ClassNotFoundException e) {
                    throw new IOException(StringUtils.toString("Read response data failed.", e));
                }
                break;
            case RESPONSE_WITH_EXCEPTION:
                try {
                    Object obj = in.readObject();
                    if (obj instanceof Throwable == false) throw new IOException("Response data error, expect Throwable, but get " + obj);
                    result.setException((Throwable) obj);
                } catch (ClassNotFoundException e) {
                    throw new IOException(StringUtils.toString("Read response data failed.", e));
                }
                break;
            default:
                throw new IOException("Unknown result flag, expect '0' '1' '2', get " + flag);
        }
        return result;
    }
}