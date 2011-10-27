package com.alibaba.dubbo.rpc.protocol.hessian;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.dubbo.remoting.http.HttpProcessor;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.protocol.AbstractExporter;

/**
 * hessian rpc exporter.
 * 
 * @author qian.lei
 */

public class HessianRpcExporter<T> extends AbstractExporter<T> implements HttpProcessor
{
	private HessianSkeletonInvoker mSkeleton;

	public HessianRpcExporter(Invoker<T> invoker)
	{
		super(invoker);
		mSkeleton = new HessianSkeletonInvoker(invoker.getInterface(), this);
	}

	public void invoke(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if( request.getMethod().equalsIgnoreCase("POST") == false )
		{
			response.setStatus(500);
		}
		else
		{
			RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
			try
			{
				mSkeleton.invoke(request.getInputStream(), response.getOutputStream());
			}
			catch(Throwable e)
			{
				throw new ServletException(e);
			}
		}
	}

}
