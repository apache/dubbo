package com.alibaba.dubbo.rpc.protocol.hessian;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.alibaba.dubbo.rpc.RpcInvocation;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.services.server.AbstractSkeleton;
import com.caucho.services.server.ServiceContext;

class HessianSkeletonInvoker extends AbstractSkeleton {

    private HessianRpcExporter<?> mExporter;

    HessianSkeletonInvoker(Class<?> serviceType, HessianRpcExporter<?> exporter) {
        super(serviceType);

        mExporter = exporter;
    }

    public void invoke(InputStream is, OutputStream os) throws Throwable {
        Hessian2Input in = new Hessian2Input(is);

        //		if (this.serializerFactory != null) {
        //			in.setSerializerFactory(this.serializerFactory);
        //		}

        int code = in.read();
        if (code != 'c')
            throw new IOException("expected 'c' in hessian input at " + code);

        AbstractHessianOutput out;
        int major = in.read();
        in.read(); // minor version, skip it.
        if (major >= 2)
            out = new Hessian2Output(os);
        else
            out = new HessianOutput(os);

        //		if (this.serializerFactory != null) {
        //			out.setSerializerFactory(this.serializerFactory);
        //		}

        // see com.alibaba.dubbo.rpc.http.hessian.v3_2_0.hessian.server.HessianSkeleton
        ServiceContext context = ServiceContext.getContext();

        // backward compatibility for some frameworks that don't read
        // the call type first
        in.skipOptionalCall();

        String header;
        while ((header = in.readHeader()) != null) {
            Object value = in.readObject();

            context.addHeader(header, value);
        }

        String methodName = in.readMethod();
        Method method = getMethod(methodName);

        if (method != null) {
        } else if ("_hessian_getAttribute".equals(methodName)) {
            String attrName = in.readString();
            in.completeCall();

            String value = null;

            if ("java.api.class".equals(attrName))
                value = getAPIClassName();
            else if ("java.home.class".equals(attrName))
                value = getHomeClassName();
            else if ("java.object.class".equals(attrName))
                value = getObjectClassName();

            out.startReply();

            out.writeObject(value);

            out.completeReply();
            out.close();
            return;
        } else if ((method = HessianUtils.getFrameworkMethod(methodName)) == null) {
            out.startReply();
            out.writeFault("NoSuchMethodException",
                    "The service has no method named: " + in.getMethod(), null);
            out.completeReply();
            out.close();
            return;
        }

        Class<?>[] args = method.getParameterTypes();
        Object[] values = new Object[args.length];

        for (int i = 0; i < args.length; i++) {
            values[i] = in.readObject(args[i]);
        }

        Object result = null;

        try {

            RpcInvocation inv = new RpcInvocation(method, values);
            result = mExporter.getInvoker().invoke(inv).recreate();

        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }
            out.startReply();
            out.writeFault("ServiceException", e.getMessage(), e);
            out.completeReply();
            out.close();
            return;
        }

        // The complete call needs to be after the invoke to handle a
        // trailing InputStream
        in.completeCall();

        out.startReply();

        out.writeObject(result);

        out.completeReply();
        out.close();
    }
    
}
