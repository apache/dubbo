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
package com.alibaba.dubbo.rpc.protocol.hessian;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.protocol.AbstractExporter;
import com.caucho.hessian.server.HessianSkeleton;

/**
 * hessian rpc exporter.
 * 
 * @author qian.lei
 */
public class HessianRpcExporter<T> extends AbstractExporter<T> implements HttpHandler {

    private HessianSkeleton skeleton;

    public HessianRpcExporter(Invoker<T> invoker, ProxyFactory proxyFactory) {
        super(invoker);
        skeleton = new HessianSkeleton(proxyFactory.getProxy(invoker), invoker.getInterface());
    }

    public void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (! request.getMethod().equalsIgnoreCase("POST")) {
            response.setStatus(500);
        } else {
            RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(),
                    request.getRemotePort());
            try {
                skeleton.invoke(request.getInputStream(), response.getOutputStream());
            } catch (Throwable e) {
                throw new ServletException(e);
            }
        }
    }

}