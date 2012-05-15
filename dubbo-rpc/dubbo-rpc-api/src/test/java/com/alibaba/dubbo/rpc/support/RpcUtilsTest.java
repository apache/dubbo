package com.alibaba.dubbo.rpc.support;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.RpcInvocation;

public class RpcUtilsTest {

	/**
	 * 正常场景：url中表示了方法异步调用
	 * 验证：1. invocationId是否正常设置,2.幂等测试
	 */
	@Test
	public void testAttachInvocationIdIfAsync_normal() {
		URL url = URL.valueOf("dubbo://localhost/?test.async=true");
		Map<String,String> attachments = new HashMap<String,String>();
		attachments.put("aa", "bb");
		Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{}, attachments);
		RpcUtils.attachInvocationIdIfAsync(url, inv);
		long id1 = RpcUtils.getInvocationId(inv);
		RpcUtils.attachInvocationIdIfAsync(url, inv);
		long id2 = RpcUtils.getInvocationId(inv);
		Assert.assertTrue( id1 == id2); //幂等操作验证
		Assert.assertTrue( id1 >= 0);
		Assert.assertEquals("bb", attachments.get("aa"));
	}
	
	/**
	 * 场景：同步调用，不默认添加acctachment
	 * 验证：acctachment中没有添加id属性
	 */
	@Test
	public void testAttachInvocationIdIfAsync_sync() {
		URL url = URL.valueOf("dubbo://localhost/");
		Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{});
		RpcUtils.attachInvocationIdIfAsync(url, inv);
		Assert.assertNull(RpcUtils.getInvocationId(inv));
	}
	
	/**
	 * 场景：异步调用，默认添加attachement
	 * 验证：当原始acctachment为null时，不能报错.
	 */
	@Test
	public void testAttachInvocationIdIfAsync_nullAttachments() {
		URL url = URL.valueOf("dubbo://localhost/?test.async=true");
		Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{});
		RpcUtils.attachInvocationIdIfAsync(url, inv);
		Assert.assertTrue(RpcUtils.getInvocationId(inv) >= 0l);
	}
	
	/**
	 * 场景：强制设置为不添加
	 * 验证：acctachment中没有添加id属性
	 */
	@Test
	public void testAttachInvocationIdIfAsync_forceNotAttache() {
		URL url = URL.valueOf("dubbo://localhost/?test.async=true&"+Constants.AUTO_ATTACH_INVOCATIONID_KEY+"=false");
		Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{});
		RpcUtils.attachInvocationIdIfAsync(url, inv);
		Assert.assertNull(RpcUtils.getInvocationId(inv));
	}
	
	/**
	 * 场景：强制设置为添加
	 * 验证：acctachment中有添加id属性
	 */
	@Test
	public void testAttachInvocationIdIfAsync_forceAttache() {
		URL url = URL.valueOf("dubbo://localhost/?"+Constants.AUTO_ATTACH_INVOCATIONID_KEY+"=true");
		Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{});
		RpcUtils.attachInvocationIdIfAsync(url, inv);
		Assert.assertNotNull(RpcUtils.getInvocationId(inv));
	}
}
